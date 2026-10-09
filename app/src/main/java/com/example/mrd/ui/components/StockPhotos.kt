package com.example.mrd.ui.components

/**
 * Royalty-free food photography from Unsplash (https://unsplash.com/license), grouped by cuisine.
 * Used when a restaurant has no image URL of its own or its URL fails to load, so every card
 * still shows a real photo. `scripts/generate_restaurants.py` uses the same table to fill the
 * mock backend's `image_url`s.
 */
private const val BASE = "https://images.unsplash.com/photo-"
private const val PARAMS = "?auto=format&fit=crop&w=800&q=70"

/** Order = priority: the first group whose keyword matches the first matching cuisine wins. */
private val Groups: List<Pair<List<String>, List<String>>> = listOf(
    listOf("pizza") to listOf("1513104890138-7c749659a591", "1565299624946-b28f40a0ae38", "1604382354936-07c5d9983bd3", "1593504049359-74330189a345", "1574071318508-1cdbab80d002", "1534308983496-4fabb1a015ee"), // pizza
    listOf("pasta", "italian") to listOf("1621996346565-e3dbc646d9a9", "1611270629569-8b357cb88da9", "1546549032-9571cd6b27df", "1551892374-ecf8754cf8b0", "1608219992759-8d74ed8d76eb", "1598866594230-a7c12756260f"), // pasta
    listOf("sushi", "japanese") to listOf("1579584425555-c3ce17fd4351", "1579871494447-9811cf80d66c", "1553621042-f6e147245754", "1611143669185-af224c5e3252", "1617196034796-73dfa7b1fd56", "1563612116625-3012372fccce"), // sushi
    listOf("burger", "american") to listOf("1568901346375-23c9450c58cd", "1586190848861-99aa4a171e90", "1572802419224-296b0aeee0d9", "1550547660-d9450f859349", "1571091718767-18b5b1457add", "1607013251379-e6eecfffe234"), // burger
    listOf("steak", "grill", "braai") to listOf("1588168333986-5078d3ae3976", "1600891964092-4316c288032e", "1558030089-02acba3c214e", "1565299715199-866c917206bb", "1558030006-450675393462", "1544025162-d76694265947"), // steak
    listOf("indian", "curry") to listOf("1601050690597-df0568f70950", "1708782344490-9026aaa5eec7", "1710091691780-c7eb0dc50cf8", "1631292784640-2b24be784d5d", "1565557623262-b51c2513a641", "1631452180519-c014fe946bc7"), // indian
    listOf("chinese", "noodle", "asian") to listOf("1585032226651-759b368d7246", "1563245372-f21724e3856d", "1523905330026-b8bd1f5f320e", "1635685296916-95acaf58471f", "1651399436026-3ca4088b3d6e", "1504674900247-0877df9cc836"), // chinese
    listOf("thai") to listOf("1627308595186-e6bb36712645", "1552912470-ee2e96439539", "1568882041008-c0954e91caba", "1563379926898-05f4575a45d8"), // thai
    listOf("korean") to listOf("1498654896293-37aacf113fd9", "1590301157890-4810ed352733", "1580651315530-69c8e0026377", "1612179343574-31b219a3915c", "1708388064278-707e85eaddc0"), // korean
    listOf("mexican", "tex-mex") to listOf("1599974579688-8dbdd335c77f", "1565299585323-38d6b0865b47", "1545093149-618ce3bcf49d", "1504544750208-dc0358e63f7f", "1683062332605-4e1209d75346", "1648437595587-e6a8b0cdf1f9"), // mexican
    listOf("chicken", "portuguese") to listOf("1426869981800-95ebf51ce900", "1730900737724-5b752e1ed3dd", "1562967914-608f82629710"), // chicken
    listOf("vegan", "vegetarian", "healthy") to listOf("1547496502-affa22d38842", "1623428187969-5da2dcea5ebf", "1623489254660-db5b367881d9", "1606757819934-d61a9f7279d5", "1569760142069-bc6838de16c1", "1626711934535-9749ea30dba8"), // vegan
    listOf("seafood", "fish") to listOf("1615141982883-c7ad0e69fd62", "1651323018466-b36b7df1d2b1", "1571167366136-b57e07761625", "1672636401225-ec3d4025a4af"), // seafood
    listOf("ice cream") to listOf("1497034825429-c343d7c6a68f", "1562790879-dfde82829db0", "1629385701021-fcd568a743e8", "1560008581-09826d1de69e", "1567206563064-6f60f40a2b57", "1570197788417-0e82375c9371"), // icecream
    listOf("bakery") to listOf("1568254183919-78a4f43a2877", "1623334044303-241021148842", "1608198093002-ad4e005484ec", "1583338917451-face2751d8d5", "1555507036-ab1f4038808a", "1534432182912-63863115e106"), // bakery
    listOf("dessert") to listOf("1606890737304-57a1ca8a5b62", "1558326567-98ae2405596b", "1476887334197-56adbf254e1a", "1517427294546-5aa121f68e8a", "1588195538326-c5b1e9f80a1b", "1551024601-bec78aea704b"), // dessert
    listOf("cafe", "coffee") to listOf("1601759226606-1352c1290364", "1595273193369-54bb86334a38", "1766557050127-756f8fcb905c", "1601759226705-cf16b1630f5a", "1612888327680-affb9c271a61"), // cafe
    listOf("breakfast") to listOf("1504754524776-8f4f37790ca0", "1538220856186-0be0e085984d", "1528699633788-424224dc89b5", "1525351484163-7529414344d8", "1530610476181-d83430b64dcd", "1493770348161-369560ae357d"), // breakfast
    listOf("mediterranean", "greek", "middle eastern") to listOf("1677686707294-e5f616c39e0b", "1542814880-7e62cf14b7c8", "1523986371872-9d3ba2e2a389", "1653611540493-b3a896319fbf", "1672636402078-4b957a572e4e"), // mediterranean

)

private val General: List<String> = listOf("1600891964599-f61ba0e24092", "1651440204227-a9a5b9d19712", "1657053460900-3a12f32b592f", "1564758564527-b97d79cb27c1")

/** Deterministic: the same restaurant always gets the same photo. */
internal fun stockPhotoUrl(cuisines: List<String>, seed: String): String {
    val lowered = cuisines.map { it.lowercase() }
    val pool = lowered.firstNotNullOfOrNull { cuisine ->
        Groups.firstOrNull { (keywords, _) -> keywords.any { cuisine.contains(it) } }?.second
    } ?: General
    val id = pool[(seed.hashCode() and Int.MAX_VALUE) % pool.size]
    return BASE + id + PARAMS
}
