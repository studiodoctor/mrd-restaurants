# SOLUTION.md — design, trade-offs and how this evolves

## 1. What I built and why

A browse → details flow where the **local database is the single source of truth**. The network
never feeds the UI; it only refreshes the database, and the UI observes the database.

```
 Compose UI ──► ViewModel ──► RestaurantRepository (domain interface)
                                      │
                       ┌──────────────┴──────────────┐
                 Room + Paging 3                Retrofit/OkHttp
              (restaurants, favorites)      (mock server via interceptor)
                       ▲                              │
                       └────── refresh(): upsert ◄────┘
```

This gives most of the required behaviour for free:

- **Offline baseline.** Once a sync has succeeded, list, search, filters, details and favourites
  all work offline. A failed refresh never touches the cache.
- **Responsiveness.** Search and filters are SQL queries on Room's executor, paged, so typing,
  toggling and scrolling never wait on the network.
- **Distinct states.** *Nothing cached + loading* → skeleton. *Nothing cached + failed* → full-screen
  error with retry. *Cached + failed* → list stays, banner with retry and "updated X ago". *Filters
  match nothing* → empty state with "Clear filters".
- **Data hygiene.** Everything on the wire is nullable and records are decoded one at a time, so a
  corrupt record (one has `"rating": "n/a"`) is dropped, not fatal. Records without id/name are
  dropped, duplicate ids keep the first, out-of-range numbers become "unknown", and a 200 with zero
  usable rows counts as a failed sync so it can't wipe a good cache.

**Improvements chosen:** favourites with persistence, deep linking (`mrd://restaurant/{id}`, also
from a cold start), and preserved inputs and scroll position. Checks: `RestaurantDaoTest` (a favourite
survives a refresh that replaces the row), `DetailViewModelTest` (cold link, not found, error → retry),
`SavedStateHandle` + DataStore for rotation, process death and restart.

## 2. Decisions and trade-offs

| Decision | Why | Given up / when I'd revisit |
|---|---|---|
| Room is the source of truth | Offline for free, one read path, easy to test | Staleness. Mitigated by "updated X ago" and refresh on launch and pull-to-refresh |
| Paging 3 over a Room `PagingSource` | Smooth as the list grows, bounded memory | More moving parts than a `List` |
| `LIKE '%q%'` name search (escaped) | Simple, fine for thousands of rows | Not index-assisted. Past ~10⁵ rows: FTS or server-side search |
| Whole-list sync with stale-row deletion | Fits the brief | A real "nearby" API is paged: use `RemoteMediator` |
| Favourites in their own table | A refresh can't erase user data | One extra `LEFT JOIN` |
| Mock backend is an OkHttp interceptor | Exercises the full stack; swapping to a real host is one line | A little more code |
| `NetworkSimulator` + debug menu | Demo offline/503 without airplane mode | Belongs in a `debug` source set in production |
| Filters and sort in one SQL query with neutral "any" values | One query serves every combination | Wide signature; add indexes at larger scale |
| Menu has its own endpoint, cache and state | A menu failure never blocks the restaurant page | A second request per visit |
| Room v3 via `@AutoMigration`, no destructive fallback | Keeps favourites, compile-time checked | Schemas must be committed |
| Type-safe nav routes (`@Serializable`) | Compile-time-checked arguments | Needs Navigation 2.8+ |
| Theme tokens in `ui/theme`, no hard-coded colours | Dark mode or a rebrand is a theme change | A bit more theme code |
| Photos: Unsplash URL → cuisine stock photo → emoji gradient | Always looks designed, even offline | Hotlinked for the demo; a real app gets URLs from the backend/CDN |
| No Android types in the domain layer | Fast JVM tests, reusable across features | A few mapper functions |

## 3. Scaling to a large, multi-squad app

**Modules.** `:app` (shell, DI, top-level NavHost), `:core:{model,common,network,database,designsystem,ui,testing}`,
and per feature `:feature:x:api` (routes, contracts) + `:feature:x:impl` (screens, ViewModels,
repository, `internal`). Features depend on each other's `:api` only. A `build-logic` convention
plugin centralises Compose/Hilt/Room/test setup, CI blocks cycles and `impl → impl` edges, and
`CODEOWNERS` maps modules to squads. The packages here (`domain` / `data` / `ui`) already follow that
cut, so extraction is mechanical.

**Navigation.** Each feature exposes a route class and a `NavGraphBuilder` extension; the shell
composes them. Features navigate by route type, never by string. Deep links are declared once per route
and cross-feature navigation goes through a small `Navigator` interface.

**State.** One immutable UI state per screen from a ViewModel (`StateFlow`), events as lambdas. Each
screen is a stateful `Route` plus a stateless `Screen`, which makes previews, screenshot tests and reuse
cheap. Process-death state uses `SavedStateHandle`; restart state uses DataStore or Room.

**Data.** UI → ViewModel → repository interface → data sources. Only `:impl` knows Room and Retrofit;
DTOs and entities never reach the UI. Failures are typed (`RefreshResult` / `FailureReason`), not thrown.

**Testing.** Many fast JVM tests (mappers, ViewModels with fakes + Turbine), in-memory Room tests for
SQL, migrations and favourites. Next: repository tests with `MockWebServer`, Compose and screenshot
tests (states, long names, large fonts, RTL), and a few UiAutomator smoke journeys in CI.

**Smooth lists as data grows.** Already in place: paged reads, stable `key`s and `contentType`,
stateless cards, Coil sized to the view, debounced search. Next: `RemoteMediator`, FTS, a Baseline
Profile with Macrobenchmark/JankStats and frame-time budgets in CI, stable models for anything crossing
composable boundaries, and thumbnail URLs from the API.

**AI-assisted development.** I use AI as a fast pair, not the author of record. It helps with
scaffolding, test data and first drafts; I keep architecture, data-model and migration design,
concurrency and anything privacy-sensitive. Prompts carry the constraints (layering, null policy),
output is reviewed like a teammate's PR, tests define behaviour before I accept generated code, and
everything must compile and pass the same CI gates. If I can't explain a line, it doesn't ship.

## 4. Known limitations

- Single whole-list sync; a real feed needs location, server paging and `RemoteMediator`.
- Search is name-only `LIKE`; FTS would add prefix and tokenised matching.
- "Open now" trusts the server flag; production would compute it from hours and the device time zone.
- The details screen reuses list fields; a real one adds `GET /restaurants/{id}`. The menu already has
  its own endpoint with cache-then-refresh. Menus are read-only (no cart).
- Not done in the timebox: release signing, crash reporting, analytics, a full accessibility audit
  (basic semantics for rating, status, favourites and loading are in place). The collapsing header and
  filter sheet need screenshot tests.
