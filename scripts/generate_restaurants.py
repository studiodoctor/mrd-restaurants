#!/usr/bin/env python3
"""Generate the mock datasets served by the in-app mock backend.

Deterministic (fixed seeds) so the files can be regenerated and diffed.

    python3 scripts/generate_restaurants.py [COUNT]

Writes:
  app/src/main/assets/restaurants.json   GET /restaurants
  app/src/main/assets/menus.json         GET /restaurants/{id}/menu  (keyed by restaurant id)

The data deliberately contains realistic *dirty* records so the client's resilience is exercised:
  restaurants
    * missing rating / ETA / fee / image / cuisines / description / address / hours
    * a record with no name                 -> dropped by the mapper
    * a record with a non-numeric rating    -> dropped individually, batch survives
    * a duplicate id                        -> first one wins
    * names containing SQL LIKE wildcards   ("100% ...", "Under_Score ...")
  menus
    * r-1020: an item without a price (shown as "Price on request"), an item without a name
              (dropped) and an empty category (dropped)
    * r-1034: no categories at all          -> "menu not available yet"
    * r-1049: no menu entry                 -> the endpoint answers 404 -> error + retry
"""
import json
import pathlib
import random
import sys

COUNT = int(sys.argv[1]) if len(sys.argv) > 1 else 240
random.seed(42)

PLACES = [
    "Cape Town", "Sea Point", "Camps Bay", "Sandton", "Rosebank", "Durban North",
    "Umhlanga", "Stellenbosch", "Melville", "Parktown", "Gardens", "Woodstock",
    "Observatory", "Bryanston", "Fourways", "Constantia", "Hout Bay", "Green Point",
    "Maboneng", "Braamfontein", "Newlands", "Claremont", "Morningside", "Menlyn",
]
STREETS = [
    "Main Road", "Long Street", "Kloof Street", "Bree Street", "Jan Smuts Avenue", "Oxford Road",
    "Florida Road", "Chiappini Street", "Rivonia Road", "Vilakazi Street", "Regent Road", "Loop Street",
    "Church Street", "Beach Road", "Victoria Road", "Dorp Street",
]

# primary cuisine -> (name nouns, plausible secondary cuisines, image keyword, descriptions)
CUISINES = {
    "Steakhouse": (["Grill", "Steakhouse", "Braai House", "Chop House"], ["Grill", "Burgers"], "steak",
                   ["Flame-grilled steaks aged for 28 days, served with hand-cut chips and house-made sauces.",
                    "A neighbourhood steakhouse built around the fire: proper cuts, honest sides, generous portions."]),
    "Sushi": (["Sushi Bar", "Sushi Co.", "Maki House"], ["Japanese", "Asian"], "sushi",
              ["Hand-rolled sushi and sashimi made to order with fresh fish delivered daily.",
               "Modern Japanese kitchen: crisp tempura, delicate rolls and generous platters for sharing."]),
    "Pizza": (["Pizzeria", "Wood Fire Pizza", "Slice Co."], ["Italian", "Pasta"], "pizza",
              ["Slow-proved dough, San Marzano tomatoes and a roaring wood-fired oven.",
               "Thin, blistered Neapolitan-style pizzas with simple, seriously good toppings."]),
    "Burgers": (["Burger Joint", "Burger Bar", "Patty Co."], ["American", "Grill"], "burger",
                ["Smashed-to-order patties, toasted buns and sauces made in-house every morning.",
                 "Big, messy, unapologetic burgers with loaded fries and thick shakes on the side."]),
    "Indian": (["Tandoor", "Curry House", "Spice Route"], ["Curry", "Vegetarian"], "curry",
               ["Aromatic curries simmered for hours and breads straight from the tandoor.",
                "Family recipes from across India with fresh spices ground in-house."]),
    "Chinese": (["Wok", "Dumpling House", "Dragon Kitchen"], ["Asian", "Noodles"], "chinese",
                ["Wok-fired classics, hand-folded dumplings and generous family-style portions.",
                 "Fast, fiery and fragrant: proper wok hei from a very hot kitchen."]),
    "Thai": (["Thai Kitchen", "Bangkok Street", "Lemongrass"], ["Asian", "Noodles"], "thai",
             ["Bright, balanced Thai flavours: sweet, sour, salty and hot in every bite.",
              "Street-food favourites and slow-cooked curries made with fresh herbs."]),
    "Mexican": (["Taqueria", "Cantina", "Burrito Bar"], ["Tex-Mex", "Grill"], "mexican",
                ["Tacos, burritos and sizzling fajitas with tortillas pressed fresh every day.",
                 "Casual cantina energy: big flavours, zingy salsas and ice-cold drinks."]),
    "Portuguese": (["Piri-Piri", "Flame Grill", "Peri Peri House"], ["Chicken", "Grill"], "chicken",
                   ["Flame-grilled chicken basted in our own peri-peri, from mild to melt-your-face hot.",
                    "Portuguese-inspired grill with crispy chips, spicy sauces and custard tarts."]),
    "Vegan": (["Plant Kitchen", "Green Table", "Leaf & Bean"], ["Healthy", "Vegetarian"], "vegan",
              ["100% plant-based comfort food that doesn't feel like a compromise.",
               "Colourful bowls, hearty mains and sweet treats, all made without animal products."]),
    "Seafood": (["Fish Market", "Oyster Bar", "Catch of the Day"], ["Sushi", "Grill"], "seafood",
                ["Line-caught fish, plump prawns and sweet calamari, cooked simply and served fast.",
                 "Coastal kitchen with the day's catch, cold drinks and a sea breeze on the menu."]),
    "Cafe": (["Cafe", "Roastery", "Brunch Spot"], ["Breakfast", "Bakery"], "brunch",
             ["All-day breakfasts, good coffee and cabinet cakes that vanish by lunchtime.",
              "A sunny corner cafe: slow brunches, small-batch roasts and fresh bakes."]),
    "Dessert": (["Creamery", "Sweet Tooth", "Waffle House"], ["Bakery", "Ice Cream"], "dessert",
                ["Waffles, gelato and baked-to-order treats for anyone with a sweet tooth.",
                 "Small-batch ice cream, warm cookies and shakes thick enough to need a spoon."]),
    "Mediterranean": (["Meze", "Olive Tree", "Souk"], ["Greek", "Middle Eastern"], "mediterranean",
                      ["Sharing plates, charcoal-grilled meats and warm pita from the oven.",
                       "Sun-soaked Mediterranean cooking built on olive oil, lemon and fresh herbs."]),
    "Korean": (["BBQ", "Kimchi House", "Seoul Kitchen"], ["Asian", "Grill"], "korean",
               ["Sizzling Korean BBQ, bubbling stews and crunchy fried chicken.",
                "Bold, fermented, fiery flavours with banchan to share."]),
    "Chicken": (["Chicken Shack", "Wing Factory", "Fried Chicken Co."], ["Burgers", "American"], "friedchicken",
                ["Buttermilk-brined, twice-fried chicken with crunch you can hear.",
                 "Wings, strips and sandwiches in every heat level, plus all the sides."]),
}

# primary cuisine -> three category names
CATEGORY_NAMES = {
    "Steakhouse": ("Starters", "Steaks & Grills", "Sides"),
    "Sushi": ("Starters", "Rolls & Platters", "Desserts"),
    "Burgers": ("Sides & Starters", "Burgers", "Shakes & Sweets"),
    "Indian": ("Starters", "Curries & Mains", "Breads & Sweets"),
    "Chinese": ("Starters", "Wok & Mains", "Sides & Sweets"),
    "Thai": ("Starters", "Curries & Noodles", "Sides & Sweets"),
    "Mexican": ("Starters", "Mains", "Sweets & Sides"),
    "Portuguese": ("Starters", "Flame-Grilled", "Sides & Sweets"),
    "Vegan": ("Starters", "Bowls & Mains", "Sweets & Sides"),
    "Seafood": ("Starters", "Mains", "Sides & Sweets"),
    "Cafe": ("Breakfast", "Light Lunch", "Coffee & Cake"),
    "Dessert": ("Waffles", "Ice Cream", "Cakes & Shakes"),
    "Mediterranean": ("Meze", "Mains", "Sweets & Sides"),
    "Korean": ("Starters", "Mains", "Sides & Sweets"),
    "Chicken": ("Starters", "Chicken", "Sides & Sweets"),
    "Pizza": ("Starters", "Pizzas", "Desserts"),
}

# primary cuisine -> three lists of (name, description, base price in cents)
MENU = {
    "Steakhouse": (
        [("Grilled Halloumi", "Citrus glaze, micro herbs", 6900), ("Beef Carpaccio", "Rocket, parmesan, truffle oil", 8900),
         ("Peri-Peri Calamari", "Lemon aioli", 7900), ("Bone Marrow", "Toasted sourdough, chimichurri", 8500)],
        [("Rump 300g", "Dry-aged, with chips and pepper sauce", 14500), ("Ribeye 400g", "Bone-in, herb butter", 19500),
         ("Fillet 250g", "Creamy mushroom sauce", 18500), ("Lamb Chops", "Mint jus, roast veg", 17500),
         ("Full Rack of Ribs", "Smoky basting, slap chips", 16900)],
        [("Creamed Spinach", "", 4500), ("Onion Rings", "Beer batter", 3900), ("Sweet Potato Fries", "", 3500), ("Garden Salad", "", 3500)],
    ),
    "Sushi": (
        [("Edamame", "Sea salt, chilli", 4500), ("Miso Soup", "Tofu, spring onion", 3500),
         ("Pork Gyoza", "Ponzu dip", 6500), ("Salmon Sashimi", "6 pieces, wasabi, pickled ginger", 8900)],
        [("Rainbow Roll", "8 pieces, assorted fish", 9500), ("Salmon Avo Roll", "8 pieces", 7900),
         ("Dragon Roll", "Prawn tempura, avo, eel sauce", 11500), ("Sushi Platter", "24 pieces, chef's selection", 21500)],
        [("Mochi Trio", "Matcha, mango, strawberry", 6500), ("Tempura Ice Cream", "Vanilla, honey", 5900),
         ("Dorayaki", "Red bean pancake", 4900), ("Matcha Cheesecake", "", 6500)],
    ),
    "Pizza": (
        [("Garlic Bread", "Mozzarella, parsley", 4900), ("Bruschetta", "Tomato, basil, balsamic", 5900),
         ("Caprese", "Buffalo mozzarella, basil", 7500), ("Arancini", "Saffron risotto, mozzarella", 6900)],
        [("Margherita", "San Marzano, fior di latte, basil", 8900), ("Pepperoni", "Spicy salami, mozzarella", 10500),
         ("Quattro Formaggi", "Four cheeses, honey", 11500), ("Diavola", "Chilli, salami, olives", 11900),
         ("Prosciutto & Rocket", "Parma ham, parmesan", 12900)],
        [("Tiramisu", "Espresso, mascarpone", 6500), ("Panna Cotta", "Berry compote", 5900), ("Nutella Calzone", "Hazelnut, icing sugar", 6900)],
    ),
    "Burgers": (
        [("Loaded Fries", "Cheese sauce, bacon", 6500), ("Onion Rings", "Smoky dip", 4500),
         ("Jalapeño Poppers", "Cream cheese", 5900), ("Buffalo Wings", "6 pieces, blue-cheese dip", 7500)],
        [("Classic Cheeseburger", "Beef patty, cheddar, pickles", 8900), ("Bacon Stack", "Double patty, streaky bacon", 11500),
         ("BBQ Smokehouse", "Onion ring, BBQ glaze", 10900), ("Crispy Chicken Burger", "Slaw, sriracha mayo", 9500),
         ("Veggie Burger", "Black-bean patty, avo", 9200)],
        [("Vanilla Shake", "", 5500), ("Chocolate Shake", "", 5500), ("Oreo Milkshake", "Cookie crumble", 5900), ("Brownie Sundae", "Salted caramel", 6500)],
    ),
    "Indian": (
        [("Samoosas", "Spiced potato, mint chutney", 4900), ("Onion Bhaji", "Tamarind dip", 4500),
         ("Chicken Tikka", "Tandoor-grilled", 7900), ("Paneer Tikka", "Peppers, onion", 7500)],
        [("Butter Chicken", "Creamy tomato gravy", 11900), ("Lamb Rogan Josh", "Kashmiri chilli, slow-cooked", 13900),
         ("Dal Makhani", "Slow-cooked black lentils", 8900), ("Chicken Biryani", "Basmati, raita", 11500), ("Palak Paneer", "Spinach, cottage cheese", 9900)],
        [("Garlic Naan", "", 2900), ("Jeera Rice", "", 2900), ("Gulab Jamun", "Rose syrup", 4500), ("Mango Lassi", "", 4500)],
    ),
    "Chinese": (
        [("Spring Rolls", "Vegetable, sweet chilli", 4500), ("Prawn Dumplings", "Steamed, chilli oil", 6900),
         ("Hot & Sour Soup", "Tofu, bamboo shoot", 5500), ("Crispy Wontons", "Plum sauce", 5500)],
        [("Sweet & Sour Chicken", "Pineapple, peppers", 9900), ("Beef & Broccoli", "Oyster sauce", 11500),
         ("Kung Pao Chicken", "Peanuts, dried chilli", 10500), ("Egg Fried Rice", "", 6500), ("Chow Mein", "Wok-tossed noodles", 8900)],
        [("Sesame Prawn Toast", "", 5900), ("Banana Fritters", "Honey, sesame", 5200), ("Steamed Bao", "Hoisin pork", 5500), ("Jasmine Tea", "", 2500)],
    ),
    "Thai": (
        [("Tom Yum Soup", "Prawn, lemongrass", 7500), ("Fresh Spring Rolls", "Peanut dip", 6500),
         ("Chicken Satay", "Peanut sauce", 7900), ("Thai Fish Cakes", "Cucumber relish", 6900)],
        [("Green Curry", "Coconut, bamboo, basil", 10900), ("Pad Thai", "Rice noodles, tamarind", 9900),
         ("Massaman Beef", "Potato, peanuts", 12500), ("Pad Kra Pao", "Holy basil, fried egg", 9500), ("Red Duck Curry", "Lychee, pineapple", 13900)],
        [("Jasmine Rice", "", 2900), ("Mango Sticky Rice", "Coconut cream", 6500), ("Thai Iced Tea", "", 4500), ("Coconut Ice Cream", "", 5500)],
    ),
    "Mexican": (
        [("Nachos Grande", "Cheese, jalapeños, guac", 8900), ("Guacamole & Chips", "", 6900),
         ("Quesadilla", "Cheese, peppers", 7900), ("Elote", "Grilled corn, lime, cotija", 5500)],
        [("Beef Burrito", "Rice, beans, pico de gallo", 9900), ("Chicken Fajitas", "Sizzling peppers, tortillas", 12500),
         ("Street Tacos x3", "Pulled pork, slaw", 9500), ("Enchiladas Verdes", "Salsa verde", 10900), ("Burrito Bowl", "Rice, beans, salsa", 9500)],
        [("Churros", "Chocolate dip", 5500), ("Tres Leches", "", 6500), ("Mexican Rice", "", 3500), ("Horchata", "Cinnamon rice milk", 4500)],
    ),
    "Portuguese": (
        [("Peri-Peri Prawns", "Garlic butter", 8900), ("Grilled Chouriço", "Flame-grilled", 6900),
         ("Marinated Olives", "Warm bread", 4500), ("Chicken Livers", "Garlic, lemon", 6500)],
        [("Half Chicken", "Flame-grilled, choice of heat", 9900), ("Whole Chicken", "Flame-grilled, 4 sides", 17900),
         ("Espetada Skewer", "Chicken thigh, peppers", 11900), ("Peri-Peri Wrap", "Lettuce, mayo", 7900), ("Prego Roll", "Steak, garlic butter", 9500)],
        [("Peri-Salt Chips", "", 3500), ("Portuguese Rice", "", 3500), ("Pastel de Nata", "Custard tart", 3500), ("Coleslaw", "", 3000)],
    ),
    "Vegan": (
        [("Hummus Platter", "Warm flatbread", 6900), ("Cauli Wings", "Buffalo, cashew ranch", 7500),
         ("Gazpacho", "Chilled tomato soup", 5900), ("Falafel Bites", "Tahini", 6500)],
        [("Buddha Bowl", "Quinoa, roasted veg, tahini", 10500), ("Mushroom Burger", "Beetroot bun", 11500),
         ("Jackfruit Tacos", "Pickled onion, lime", 9900), ("Creamy Cashew Pasta", "Sun-dried tomato", 10900), ("Tofu Green Curry", "Coconut, basil", 10500)],
        [("Raw Cacao Brownie", "", 5500), ("Coconut Chia Pot", "Mango", 5200), ("Green Smoothie", "Spinach, apple, ginger", 5500), ("Sweet Potato Fries", "", 4500)],
    ),
    "Seafood": (
        [("Oysters x6", "Mignonette", 14500), ("Salt & Pepper Calamari", "Lemon aioli", 8500),
         ("Prawn Cocktail", "Marie Rose", 8900), ("Seafood Chowder", "Crusty bread", 7500)],
        [("Line Fish", "Catch of the day, lemon butter", 13900), ("Prawn Platter", "Peri-peri or garlic", 21900),
         ("Fish & Chips", "Beer batter", 11500), ("Linguine Vongole", "White wine, chilli", 12900), ("Grilled Hake", "Herb butter", 12500)],
        [("Lemon Tart", "", 6500), ("Garlic Bread", "", 3900), ("Side Salad", "", 3500), ("Crème Brûlée", "", 6500)],
    ),
    "Cafe": (
        [("Eggs Benedict", "Ham, hollandaise", 8900), ("Avo Smash", "Poached eggs, feta", 8500),
         ("Granola Bowl", "Yoghurt, berries", 6500), ("French Toast", "Maple, banana", 7500)],
        [("Chicken Mayo Toastie", "", 7500), ("Club Sandwich", "Bacon, egg, chicken", 9500),
         ("Caesar Salad", "Parmesan, croutons", 8900), ("Quiche of the Day", "Side salad", 7900)],
        [("Flat White", "", 3500), ("Cappuccino", "", 3500), ("Carrot Cake", "Cream-cheese icing", 5500), ("Cheesecake Slice", "", 5900)],
    ),
    "Dessert": (
        [("Belgian Waffle", "Strawberries, cream", 7900), ("Nutella Waffle", "Hazelnuts", 8500),
         ("Waffle Sundae", "Vanilla ice cream", 9500), ("Churro Waffle", "Dulce de leche", 8900)],
        [("Two Scoops", "", 5500), ("Three Scoops", "", 7500), ("Gelato Tub", "500ml", 12500), ("Affogato", "Espresso, vanilla", 5500)],
        [("Red Velvet Slice", "", 6500), ("Fudge Brownie", "", 5500), ("Cookie Dough Skillet", "Warm, with ice cream", 8500), ("Thick Milkshake", "", 5900)],
    ),
    "Mediterranean": (
        [("Meze Platter", "Dips, olives, pita", 11900), ("Hummus & Pita", "", 5900),
         ("Halloumi Fries", "Honey, thyme", 6900), ("Stuffed Vine Leaves", "Lemon, mint", 6500)],
        [("Lamb Kofta", "Tzatziki, flatbread", 11900), ("Chicken Shawarma Plate", "Garlic sauce, pickles", 10900),
         ("Moussaka", "Aubergine, béchamel", 11500), ("Falafel Wrap", "Tahini, slaw", 7900), ("Greek Salad", "Feta, olives", 7500)],
        [("Baklava", "Pistachio, honey", 5500), ("Loukoumades", "Honey, cinnamon", 5900), ("Warm Pita", "", 2500), ("Turkish Coffee", "", 3500)],
    ),
    "Korean": (
        [("Kimchi Pancake", "", 6900), ("Pork Mandu", "Dumplings", 6500),
         ("Tteokbokki", "Spicy rice cakes", 7500), ("Gochujang Wings", "", 8500)],
        [("Bibimbap", "Rice, veg, gochujang, egg", 10900), ("Bulgogi Beef", "Marinated, lettuce wraps", 12900),
         ("Pork Belly BBQ", "Ssamjang", 13500), ("Japchae", "Glass noodles", 9500), ("Kimchi Jjigae", "Bubbling stew", 9900)],
        [("Steamed Rice", "", 2500), ("Hotteok", "Brown-sugar pancakes", 5200), ("Bingsu", "Shaved ice, red bean", 7500), ("Yuja Tea", "", 3500)],
    ),
    "Chicken": (
        [("Wings x8", "Hot, BBQ or lemon-pepper", 8900), ("Mac & Cheese Bites", "", 6500),
         ("Coleslaw", "", 3000), ("Corn on the Cob", "Chilli butter", 3500)],
        [("Chicken Bucket 8pc", "Buttermilk fried", 18900), ("Strips Combo", "Chips, sauce", 9900),
         ("Spicy Chicken Burger", "", 9500), ("Chicken Wrap", "", 8500), ("Nashville Hot", "Pickles, white bread", 10500)],
        [("Chips", "", 3500), ("Gravy", "", 1500), ("Apple Pie", "", 4500), ("Milkshake", "", 5500)],
    ),
}

DRINKS = [("Still Water", "500ml", 2500), ("Sparkling Water", "500ml", 2900), ("Cola", "330ml", 3200),
          ("Iced Tea", "Peach or lemon", 3500), ("Craft Ginger Beer", "", 4500), ("Fresh Orange Juice", "", 4900)]

OPTION_POOL = [("Extra sauce", 500), ("Make it spicy", 0), ("Add cheese", 1200), ("Large portion", 2500), ("Gluten-free option", 1500)]

STOCK_PHOTO_GROUPS = [  # same table as ui/components/StockPhotos.kt (order = priority)
    (['pizza'], ['1513104890138-7c749659a591', '1565299624946-b28f40a0ae38', '1604382354936-07c5d9983bd3', '1593504049359-74330189a345', '1574071318508-1cdbab80d002', '1534308983496-4fabb1a015ee']),
    (['pasta', 'italian'], ['1621996346565-e3dbc646d9a9', '1611270629569-8b357cb88da9', '1546549032-9571cd6b27df', '1551892374-ecf8754cf8b0', '1608219992759-8d74ed8d76eb', '1598866594230-a7c12756260f']),
    (['sushi', 'japanese'], ['1579584425555-c3ce17fd4351', '1579871494447-9811cf80d66c', '1553621042-f6e147245754', '1611143669185-af224c5e3252', '1617196034796-73dfa7b1fd56', '1563612116625-3012372fccce']),
    (['burger', 'american'], ['1568901346375-23c9450c58cd', '1586190848861-99aa4a171e90', '1572802419224-296b0aeee0d9', '1550547660-d9450f859349', '1571091718767-18b5b1457add', '1607013251379-e6eecfffe234']),
    (['steak', 'grill', 'braai'], ['1588168333986-5078d3ae3976', '1600891964092-4316c288032e', '1558030089-02acba3c214e', '1565299715199-866c917206bb', '1558030006-450675393462', '1544025162-d76694265947']),
    (['indian', 'curry'], ['1601050690597-df0568f70950', '1708782344490-9026aaa5eec7', '1710091691780-c7eb0dc50cf8', '1631292784640-2b24be784d5d', '1565557623262-b51c2513a641', '1631452180519-c014fe946bc7']),
    (['chinese', 'noodle', 'asian'], ['1585032226651-759b368d7246', '1563245372-f21724e3856d', '1523905330026-b8bd1f5f320e', '1635685296916-95acaf58471f', '1651399436026-3ca4088b3d6e', '1504674900247-0877df9cc836']),
    (['thai'], ['1627308595186-e6bb36712645', '1552912470-ee2e96439539', '1568882041008-c0954e91caba', '1563379926898-05f4575a45d8']),
    (['korean'], ['1498654896293-37aacf113fd9', '1590301157890-4810ed352733', '1580651315530-69c8e0026377', '1612179343574-31b219a3915c', '1708388064278-707e85eaddc0']),
    (['mexican', 'tex-mex'], ['1599974579688-8dbdd335c77f', '1565299585323-38d6b0865b47', '1545093149-618ce3bcf49d', '1504544750208-dc0358e63f7f', '1683062332605-4e1209d75346', '1648437595587-e6a8b0cdf1f9']),
    (['chicken', 'portuguese'], ['1426869981800-95ebf51ce900', '1730900737724-5b752e1ed3dd', '1562967914-608f82629710']),
    (['vegan', 'vegetarian', 'healthy'], ['1547496502-affa22d38842', '1623428187969-5da2dcea5ebf', '1623489254660-db5b367881d9', '1606757819934-d61a9f7279d5', '1569760142069-bc6838de16c1', '1626711934535-9749ea30dba8']),
    (['seafood', 'fish'], ['1615141982883-c7ad0e69fd62', '1651323018466-b36b7df1d2b1', '1571167366136-b57e07761625', '1672636401225-ec3d4025a4af']),
    (['ice cream'], ['1497034825429-c343d7c6a68f', '1562790879-dfde82829db0', '1629385701021-fcd568a743e8', '1560008581-09826d1de69e', '1567206563064-6f60f40a2b57', '1570197788417-0e82375c9371']),
    (['bakery'], ['1568254183919-78a4f43a2877', '1623334044303-241021148842', '1608198093002-ad4e005484ec', '1583338917451-face2751d8d5', '1555507036-ab1f4038808a', '1534432182912-63863115e106']),
    (['dessert'], ['1606890737304-57a1ca8a5b62', '1558326567-98ae2405596b', '1476887334197-56adbf254e1a', '1517427294546-5aa121f68e8a', '1588195538326-c5b1e9f80a1b', '1551024601-bec78aea704b']),
    (['cafe', 'coffee'], ['1601759226606-1352c1290364', '1595273193369-54bb86334a38', '1766557050127-756f8fcb905c', '1601759226705-cf16b1630f5a', '1612888327680-affb9c271a61']),
    (['breakfast'], ['1504754524776-8f4f37790ca0', '1538220856186-0be0e085984d', '1528699633788-424224dc89b5', '1525351484163-7529414344d8', '1530610476181-d83430b64dcd', '1493770348161-369560ae357d']),
    (['mediterranean', 'greek', 'middle eastern'], ['1677686707294-e5f616c39e0b', '1542814880-7e62cf14b7c8', '1523986371872-9d3ba2e2a389', '1653611540493-b3a896319fbf', '1672636402078-4b957a572e4e']),
]
STOCK_PHOTO_GENERAL = ['1600891964599-f61ba0e24092', '1651440204227-a9a5b9d19712', '1657053460900-3a12f32b592f', '1564758564527-b97d79cb27c1']

STOCK_PHOTO_BASE = "https://images.unsplash.com/photo-"
STOCK_PHOTO_PARAMS = "?auto=format&fit=crop&w=800&q=70"


def stock_photo(cuisines: list, index: int) -> str:
    """A real Unsplash food photo for the cuisines; `index` spreads restaurants across the pool."""
    pool = STOCK_PHOTO_GENERAL
    lowered = [c.lower() for c in cuisines]
    for cuisine in lowered:
        group = next((ids for keywords, ids in STOCK_PHOTO_GROUPS if any(k in cuisine for k in keywords)), None)
        if group:
            pool = group
            break
    return STOCK_PHOTO_BASE + pool[index % len(pool)] + STOCK_PHOTO_PARAMS

TAG_POOL = ["Halaal", "Vegetarian friendly", "Vegan options", "Family favourite", "Great for groups",
            "Healthy", "Spicy", "Gluten-free options", "Late night", "Comfort food"]

FEES = [0, 0, 1500, 1500, 1900, 2500, 2900, 3500, 4500]
OPENS = ["07:00", "08:00", "09:00", "10:00", "11:00", "12:00"]
CLOSES = ["20:00", "21:00", "22:00", "23:00", "23:30"]


def build_restaurants(count: int) -> list:
    rows, names = [], set()
    primaries = list(CUISINES)
    for i in range(count):
        primary = primaries[i % len(primaries)] if i < len(primaries) else random.choice(primaries)
        nouns, secondaries, keyword, descriptions = CUISINES[primary]
        place = None
        for _ in range(20):
            place = random.choice(PLACES)
            name = f"{place} {random.choice(nouns)}"
            if name not in names:
                break
        else:
            name = f"{name} {i}"
        names.add(name)
        rid = f"r-{1001 + i}"
        cuisines = [primary]
        if random.random() < 0.75:
            cuisines.append(random.choice(secondaries))
        rating = round(random.uniform(3.2, 4.9), 1)
        tags = random.sample(TAG_POOL, random.choice([0, 1, 2, 2, 3]))
        if primary == "Vegan" and "Vegan options" not in tags:
            tags.append("Vegan options")
        if rating >= 4.7:
            tags.insert(0, "Top rated")
        suburb = random.choice(PLACES)
        rows.append({
            "id": rid,
            "name": name,
            "cuisines": cuisines,
            "rating": rating,
            "delivery_fee_cents": random.choice(FEES),
            "eta_minutes": random.randint(15, 65),
            "is_open": random.random() < 0.72,
            # Every restaurant gets a real Unsplash photo. (random.random() is still consumed so the
            # rest of the deterministic data is unchanged from earlier versions.)
            "image_url": stock_photo(cuisines, i + int(random.random() * 0)),
            # Own RNG so adding this field leaves the rest of the seeded data unchanged.
            "distance_km": round(random.Random(f"distance-{i}").uniform(0.4, 18.0), 1),
            "neighbourhood": suburb,
            "description": random.choice(descriptions),
            "address": f"{random.randint(1, 240)} {random.choice(STREETS)}, {suburb}",
            "opens_at": random.choice(OPENS),
            "closes_at": random.choice(CLOSES),
            "price_level": random.choices([1, 2, 3, 4], weights=[3, 5, 3, 1])[0],
            "review_count": random.randint(18, 2400),
            "min_order_cents": random.choice([0, 0, 5000, 7500, 10000, 15000]),
            "tags": tags,
        })

    # --- sprinkle missing / null fields (about 4-6% each) -------------------
    def null_out(field: str, n: int) -> None:
        for idx in random.sample(range(len(rows)), n):
            rows[idx][field] = None

    null_out("rating", 12)
    null_out("eta_minutes", 9)
    null_out("delivery_fee_cents", 8)
    null_out("neighbourhood", 15)
    null_out("description", 10)
    null_out("address", 12)
    null_out("review_count", 14)
    null_out("price_level", 10)
    null_out("min_order_cents", 10)
    for idx in random.sample(range(len(rows)), 8):
        rows[idx]["opens_at"] = rows[idx]["closes_at"] = None
    rows[11]["cuisines"] = None
    rows[44]["cuisines"] = []
    del rows[70]["is_open"]  # missing flag -> treated as closed
    rows[80].pop("tags")     # tags missing entirely

    # --- wildcard names to exercise LIKE escaping -------------------------------
    rows[5]["name"] = "100% Vegan Kitchen"
    rows[5]["cuisines"] = ["Vegan", "Healthy"]
    rows[6]["name"] = "Under_Score Eats"

    # --- invalid records the client must survive -------------------------------
    poison = {"id": "r-bad-rating", "name": "Corrupt Rating Cafe", "cuisines": ["Cafe"],
              "rating": "n/a", "is_open": True}
    no_name = {"id": "r-no-name", "cuisines": ["Pizza"], "rating": 4.0, "is_open": True}
    duplicate = dict(rows[10])
    duplicate["name"] = "Duplicate Of r-1011 (should be ignored)"
    rows.insert(30, poison)
    rows.insert(55, no_name)
    rows.insert(90, duplicate)
    return rows


def build_menu(restaurant: dict) -> dict:
    rid = restaurant["id"]
    number = rid.split("-")[1]
    rng = random.Random(f"menu-{rid}")
    primary = next((c for c in (restaurant.get("cuisines") or []) if c in MENU), None)
    if primary is None:
        primary = rng.choice(list(MENU))
    names = CATEGORY_NAMES[primary]
    categories = []
    for ci, (cat_name, pool) in enumerate(zip(names, MENU[primary])):
        picks = rng.sample(pool, rng.randint(min(3, len(pool)), len(pool)))
        items = [make_item(number, ci, ii, rng, *pick) for ii, pick in enumerate(picks)]
        categories.append({"id": f"c-{number}-{ci}", "name": cat_name, "items": items})
    if rng.random() < 0.85:
        picks = rng.sample(DRINKS, rng.randint(3, 5))
        items = [make_item(number, 3, ii, rng, *pick) for ii, pick in enumerate(picks)]
        categories.append({"id": f"c-{number}-3", "name": "Drinks", "items": items})
    return {"restaurant_id": rid, "categories": categories}


def make_item(number: str, ci: int, ii: int, rng: random.Random, name: str, description: str, base: int) -> dict:
    price = int(round(base * rng.uniform(0.92, 1.22) / 50.0)) * 50
    item = {
        "id": f"i-{number}-{ci}{ii}",
        "name": name,
        "price_cents": price,
        "available": rng.random() < 0.92,
    }
    if description:
        item["description"] = description
    if rng.random() < 0.25:
        options = rng.sample(OPTION_POOL, rng.randint(1, 2))
        item["options"] = [{"id": f"o-{number}-{ci}{ii}{k}", "name": n, "price_cents": p}
                           for k, (n, p) in enumerate(options)]
    return item


def build_menus(rows: list) -> dict:
    menus = {}
    for r in rows:
        if not r["id"].startswith("r-1") or "name" not in r or r.get("rating") == "n/a":
            continue
        if r["id"] in menus:
            continue
        menus[r["id"]] = build_menu(r)

    # --- dirty menu cases ------------------------------------------------------
    bad = menus["r-1020"]["categories"]
    bad[0]["items"][0].pop("price_cents")              # price on request
    bad[0]["items"].append({"id": "i-bad-name", "price_cents": 1000, "available": True})  # no name -> dropped
    bad.append({"id": "c-empty", "name": "Empty Category", "items": []})                # dropped
    menus["r-1034"]["categories"] = []                 # no menu yet
    del menus["r-1049"]                                # 404 from the endpoint
    return menus


if __name__ == "__main__":
    rows = build_restaurants(COUNT)
    assets = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)

    (assets / "restaurants.json").write_text(
        json.dumps({"version": 2, "restaurants": rows}, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")

    menus = build_menus(rows)
    (assets / "menus.json").write_text(
        json.dumps({"version": 1, "menus": menus}, separators=(",", ":"), ensure_ascii=False) + "\n", encoding="utf-8")

    valid = {r["id"] for r in rows if isinstance(r.get("name"), str) and r.get("rating") != "n/a"}
    items = sum(len(c["items"]) for m in menus.values() for c in m["categories"])
    print(f"restaurants.json: {len(rows)} records ({len(valid)} unique valid)")
    print(f"menus.json: {len(menus)} menus, {items} items")
