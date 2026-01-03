#!/usr/bin/env python3
"""
Standalone Python script to populate TCG Arena shops database using Google Places API
Alternative to the Java endpoint - can be run independently

Usage:
    python populate_shops_script.py --api-key YOUR_GOOGLE_API_KEY --db-url jdbc:mysql://localhost:3306/tcg_arena --db-user root --db-password password
    
Or use environment variables:
    export GOOGLE_PLACES_API_KEY=your_key
    export DB_URL=jdbc:mysql://localhost:3306/tcg_arena
    export DB_USER=root
    export DB_PASSWORD=password
    python populate_shops_script.py
"""

import requests
import time
import argparse
import os
import sys
from typing import List, Dict, Tuple
import mysql.connector
from mysql.connector import Error

# Italian cities with coordinates - Focus on Italy only to optimize API quota
ITALIAN_CITIES = {
    # Lazio
    "Roma, Italia": (41.9028, 12.4964),
    "Latina, Italia": (41.4677, 12.9036),
    "Frosinone, Italia": (41.6395, 13.3509),
    "Viterbo, Italia": (42.4173, 12.1075),
    
    # Lombardia
    "Milano, Italia": (45.4642, 9.1900),
    "Bergamo, Italia": (45.6983, 9.6773),
    "Brescia, Italia": (45.5416, 10.2118),
    "Monza, Italia": (45.5845, 9.2744),
    "Como, Italia": (45.8081, 9.0852),
    "Varese, Italia": (45.8206, 8.8251),
    "Pavia, Italia": (45.1847, 9.1582),
    "Cremona, Italia": (45.1335, 10.0226),
    "Mantova, Italia": (45.1564, 10.7914),
    "Lecco, Italia": (45.8559, 9.3988),
    
    # Campania
    "Napoli, Italia": (40.8518, 14.2681),
    "Salerno, Italia": (40.6824, 14.7681),
    "Caserta, Italia": (41.0732, 14.3328),
    "Avellino, Italia": (40.9142, 14.7906),
    "Benevento, Italia": (41.1297, 14.7820),
    
    # Piemonte
    "Torino, Italia": (45.0703, 7.6869),
    "Alessandria, Italia": (44.9133, 8.6152),
    "Asti, Italia": (44.9009, 8.2065),
    "Cuneo, Italia": (44.3841, 7.5426),
    "Novara, Italia": (45.4469, 8.6218),
    
    # Toscana
    "Firenze, Italia": (43.7696, 11.2558),
    "Pisa, Italia": (43.7228, 10.4017),
    "Livorno, Italia": (43.5485, 10.3106),
    "Prato, Italia": (43.8777, 11.1022),
    "Lucca, Italia": (43.8376, 10.4950),
    "Arezzo, Italia": (43.4632, 11.8796),
    "Siena, Italia": (43.3188, 11.3308),
    "Pistoia, Italia": (43.9330, 10.9177),
    "Grosseto, Italia": (42.7634, 11.1138),
    
    # Emilia-Romagna
    "Bologna, Italia": (44.4949, 11.3426),
    "Modena, Italia": (44.6471, 10.9252),
    "Parma, Italia": (44.8015, 10.3279),
    "Reggio Emilia, Italia": (44.6989, 10.6297),
    "Ferrara, Italia": (44.8381, 11.6198),
    "Ravenna, Italia": (44.4183, 12.2035),
    "Rimini, Italia": (44.0678, 12.5695),
    "Forlì, Italia": (44.2226, 12.0408),
    "Cesena, Italia": (44.1397, 12.2433),
    "Piacenza, Italia": (45.0526, 9.6929),
    
    # Veneto
    "Venezia, Italia": (45.4408, 12.3155),
    "Verona, Italia": (45.4384, 10.9916),
    "Padova, Italia": (45.4064, 11.8768),
    "Vicenza, Italia": (45.5455, 11.5354),
    "Treviso, Italia": (45.6669, 12.2430),
    "Rovigo, Italia": (45.0703, 11.7898),
    "Belluno, Italia": (46.1387, 12.2165),
    
    # Liguria
    "Genova, Italia": (44.4056, 8.9463),
    "La Spezia, Italia": (44.1027, 9.8244),
    "Savona, Italia": (44.3080, 8.4813),
    "Imperia, Italia": (43.8876, 8.0271),
    
    # Sicilia
    "Palermo, Italia": (38.1157, 13.3615),
    "Catania, Italia": (37.5079, 15.0830),
    "Messina, Italia": (38.1938, 15.5540),
    "Siracusa, Italia": (37.0755, 15.2866),
    "Trapani, Italia": (38.0176, 12.5365),
    "Agrigento, Italia": (37.3109, 13.5765),
    
    # Puglia
    "Bari, Italia": (41.1171, 16.8719),
    "Taranto, Italia": (40.4762, 17.2403),
    "Foggia, Italia": (41.4621, 15.5446),
    "Lecce, Italia": (40.3515, 18.1750),
    "Brindisi, Italia": (40.6327, 17.9417),
    
    # Calabria
    "Reggio Calabria, Italia": (38.1113, 15.6473),
    "Catanzaro, Italia": (38.9098, 16.5877),
    "Cosenza, Italia": (39.2986, 16.2520),
    
    # Sardegna
    "Cagliari, Italia": (39.2238, 9.1217),
    "Sassari, Italia": (40.7259, 8.5594),
    "Olbia, Italia": (40.9237, 9.5034),
    
    # Marche
    "Ancona, Italia": (43.6158, 13.5189),
    "Pesaro, Italia": (43.9103, 12.9133),
    "Macerata, Italia": (43.2998, 13.4532),
    "Ascoli Piceno, Italia": (42.8542, 13.5759),
    
    # Umbria
    "Perugia, Italia": (43.1107, 12.3908),
    "Terni, Italia": (42.5633, 12.6466),
    
    # Abruzzo
    "L'Aquila, Italia": (42.3498, 13.3995),
    "Pescara, Italia": (42.4618, 14.2159),
    "Teramo, Italia": (42.6589, 13.7039),
    "Chieti, Italia": (42.3512, 14.1677),
    
    # Friuli-Venezia Giulia
    "Trieste, Italia": (45.6495, 13.7768),
    "Udine, Italia": (46.0710, 13.2345),
    "Pordenone, Italia": (45.9636, 12.6606),
    
    # Trentino-Alto Adige
    "Trento, Italia": (46.0664, 11.1257),
    "Bolzano, Italia": (46.4983, 11.3548),
    
    # Valle d'Aosta
    "Aosta, Italia": (45.7372, 7.3205),
    
    # Molise
    "Campobasso, Italia": (41.5630, 14.6560),
    
    # Basilicata
    "Potenza, Italia": (40.6420, 15.7990),
    "Matera, Italia": (40.6664, 16.6043),
}

SEARCH_KEYWORDS = [
    "pokemon card shop",
    "magic the gathering shop",
    "yugioh card shop",
    "trading card game shop",
    "TCG shop",
    "collectible card game store",
    "fumetteria carte",
    "negozio carte pokemon",
    "negozio giochi da tavolo carte",
]


def search_nearby_shops(api_key: str, lat: float, lng: float, keyword: str, radius: int = 15000) -> List[Dict]:
    """Search for shops using Google Places Nearby Search API"""
    url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    params = {
        "location": f"{lat},{lng}",
        "radius": radius,
        "keyword": keyword,
        "key": api_key
    }
    
    try:
        response = requests.get(url, params=params)
        data = response.json()
        
        if data.get("status") not in ["OK", "ZERO_RESULTS"]:
            print(f"Warning: API returned status {data.get('status')}")
            return []
        
        return data.get("results", [])
    except Exception as e:
        print(f"Error searching places: {e}")
        return []


def get_place_details(api_key: str, place_id: str) -> Dict:
    """Get detailed information about a place"""
    url = "https://maps.googleapis.com/maps/api/place/details/json"
    params = {
        "place_id": place_id,
        "fields": "formatted_phone_number,website,opening_hours,formatted_address,editorial_summary,reviews,rating,user_ratings_total",
        "key": api_key
    }
    
    try:
        response = requests.get(url, params=params)
        data = response.json()
        
        if data.get("status") == "OK":
            return data.get("result", {})
        return {}
    except Exception as e:
        print(f"Error getting place details: {e}")
        return {}


def detect_tcg_types_and_services(name: str, place_types: List[str], description: str = "") -> tuple:
    """Detect TCG types and services from multiple sources"""
    # Combine all text sources
    combined_text = f"{name.lower()} {description.lower()} {' '.join(place_types).lower()}"
    
    tcg_types = []
    services = []
    
    # Detect TCG types
    if "pokemon" in combined_text or "pokémon" in combined_text:
        tcg_types.append("POKEMON")
    if "magic" in combined_text or "mtg" in combined_text or "gathering" in combined_text:
        tcg_types.append("MAGIC")
    if "yugioh" in combined_text or "yu-gi-oh" in combined_text or "yu gi oh" in combined_text:
        tcg_types.append("YUGIOH")
    if "one piece" in combined_text:
        tcg_types.append("ONE_PIECE")
    if "dragon ball" in combined_text:
        tcg_types.append("DRAGON_BALL")
    if "lorcana" in combined_text:
        tcg_types.append("LORCANA")
    
    # If none detected, add all (generic card shop)
    if not tcg_types:
        tcg_types = ["POKEMON", "MAGIC", "YUGIOH", "ONE_PIECE", "DRAGON_BALL", "LORCANA"]
    
    # Detect services
    # Check business status
    if place.get("business_status") != "OPERATIONAL":
        print(f"  ⊘ Skipping non-operational: {place.get('name', 'Unknown')}")
        return None
    
    # Check rating
    rating = place.get("rating", 0)
    if rating > 0 and rating < 3.0:
        print(f"  ⊘ Skipping low-rated ({rating}): {place.get('name', 'Unknown')}")
        return None
    
    shop = {
        "name": place.get("name", ""),
        "address": place.get("vicinity", ""),
        "latitude": place.get("geometry", {}).get("location", {}).get("lat"),
        "longitude": place.get("geometry", {}).get("location", {}).get("lng"),
        "type": "STORE",
        "is_verified": False,
        "active": False,
        "phone_number": None,
        "website_url": None,
        "description": None,
    }
    
    # Get place types
    place_types = place.get("types", [])
    
    # Get additional details
    if "place_id" in place:
        details = get_place_details(api_key, place["place_id"])
        shop["phone_number"] = details.get("formatted_phone_number")
        shop["website_url"] = details.get("website")
        if "formatted_address" in details:
            shop["address"] = details["formatted_address"]
        
        # Extract description
        if "editorial_summary" in details:
            shop["description"] = details["editorial_summary"].get("overview", "")
        elif "reviews" in details and len(details["reviews"]) > 0:
            review_text = details["reviews"][0].get("text", "")
            shop["description"] = review_text[:500] if len(review_text) > 500 else review_text
        
        time.sleep(0.5)  # Rate limiting
    
    # Detect TCG types and services from multiple sources
    tcg_types, services = detect_tcg_types_and_services(
        shop["name"], 
        place_types,
        shop.get("description", "")
    )
    
    shop["tcg_types"] = tcg_types
    shop["services"] = services


def parse_shop_from_place(place: Dict, api_key: str) -> Dict:
    """Parse Google Places result into shop data"""
    shop = {
        "name": place.get("name", ""),
        "address": place.get("vicinity", ""),
        "latitude": place.get("geometry", {}).get("location", {}).get("lat"),
        "longitude": place.get("geometry", {}).get("location", {}).get("lng"),
        "type": "STORE",
        "is_verified": False,
        "active": False,
        "phone_number": None,
        "website_url": None,
    }
    
    # Get additional details
    if "place_id" in place:
        details = get_place_details(api_key, place["place_id"])
        shop["phone_number"] = details.get("formatted_phone_number")
        shop["website_url"] = details.get("website")
        if "formatted_address" in details:
            shop["address"] = details["formatted_address"]
        time.sleep(0.5)  # Rate limiting
    
    # Detect TCG types
    shop["tcg_types"] = detect_tcg_types(shop["name"])
    shop["services"] = "BUY_CARDS,SELL_CARDS,TRADE,TOURNAMENTS,SEALED_PRODUCTS,ACCESSORIES,PLAY_AREA"
    
    return shop


def connect_to_database(db_config: Dict):
    """Connect to MySQL database"""
    try:
        # Parse JDBC URL if provided
        if db_config.get("url", "").startswith("jdbc:mysql://"):
            jdbc_url = db_config["url"].replace("jdbc:mysql://", "")
            parts = jdbc_url.split("/")
            host_port = parts[0].split(":")
            database = parts[1].split("?")[0] if len(parts) > 1 else "tcg_arena"
            
            host = host_port[0]
            port = int(host_port[1]) if len(host_port) > 1 else 3306
        else:
            host = db_config.get("host", "localhost")
            port = db_config.get("port", 3306)
            database = db_config.get("database", "tcg_arena")
        
        connection = mysql.connector.connect(
            host=host,
            port=port,
            database=database,
            user=db_config["user"],
            password=db_config["password"]
        )
        
        if connection.is_connected():
            print(f"✓ Connected to MySQL database: {database}")
            return connection
    except Error as e:
        print(f"Error connecting to MySQL: {e}")
        return None


def shop_exists(cursor, shop: Dict) -> bool:
    """Check if shop already exists in database"""
    query = """
        SELECT COUNT(*) FROM shops 
        WHERE LOWER(name) = LOWER(%s)
        AND (
            (latitude BETWEEN %s AND %s) AND 
            (longitude BETWEEN %s AND %s)
        )
    """
    # Check within ~100 meters (roughly 0.001 degrees)
    lat_range = 0.001
    lng_range = 0.001
    
    cursor.execute(query, (
        shop["name"],
        shop["latitude"] - lat_range,
        shop["latitude"] + lat_range,
        shop["longitude"] - lng_range,
        shop["longitude"] + lng_range
    ))
    
    count = cursor.fetchone()[0]
    return count > 0


def insert_shop(cursor, shop: Dict) -> bool:
    """Insert shop into database"""
    query = """
        INSERT INTO shops (
            name, address, latitude, longitude, phone_number, website_url,
            type, is_verified, active, tcg_types, services, reservation_duration_minutes
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    
    try:
        cursor.execute(query, (
            shop["name"],
            shop["address"],
            shop["latitude"],
            shop["longitude"],
            shop.get("phone_number"),
            shop.get("website_url"),
            shop["type"],
            shop["is_verified"],
            shop["active"],
            shop["tcg_types"],
            shop["services"],
            30  # default reservation duration
        ))
        return True
    except Error as e:
        print(f"Error inserting shop {shop['name']}: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description="Populate TCG Arena shops from Google Places")
    parser.add_argument("--api-key", help="Google Places API key", default=os.getenv("GOOGLE_PLACES_API_KEY"))
    parser.add_argument("--db-url", help="Database JDBC URL", default=os.getenv("DB_URL"))
    parser.add_argument("--db-user", help="Database user", default=os.getenv("DB_USER", "root"))
    parser.add_argument("--db-password", help="Database password", default=os.getenv("DB_PASSWORD"))
    parser.add_argument("--dry-run", action="store_true", help="Simulate without inserting data")
    parser.add_argument("--max-requests", type=int, default=950, help="Maximum API requests (default: 950)")
    parser.add_argument("--skip-details", action="store_true", help="Skip Place Details calls to save quota")
    
    args = parser.parse_args()
    
    if not args.api_key:
        print("Error: Google Places API key is required")
        print("Provide via --api-key or GOOGLE_PLACES_API_KEY environment variable")
        sys.exit(1)
    
    if not args.dry_run:
        if not args.db_url or not args.db_password:
            print("Error: Database credentials are required for non-dry-run mode")
            sys.exit(1)
        
        db_config = {
            "url": args.db_url,
            "user": args.db_user,
            "passwSkip if shop was filtered out (non-operational, low rating, etc.)
                if shop is None:
                    continue
                
                # ord": args.db_password
        }
        
        connection = connect_to_database(db_config)
        if not connection:
            sys.exit(1)
        cursor = connection.cursor()
    else:
        print("🔍 DRY RUN MODE - No data will be inserted\n")
        connection = None
        cursor = None
    
    total_found = 0
    total_inserted = 0
    total_skipped = 0
    api_calls_nearby = 0
    api_calls_details = 0
    
    print(f"Starting shop population for ITALY - {len(ITALIAN_CITIES)} cities...")
    print(f"Using {len(SEARCH_KEYWORDS)} search keywords")
    print(f"API Request Limit: {args.max_requests}")
    print(f"Skip Place Details: {args.skip_details}\n")
    
    for city_name, (lat, lng) in ITALIAN_CITIES.items():
        # Check if we've reached the limit
        total_api_calls = api_calls_nearby + api_calls_details
        if total_api_calls >= args.max_requests:
            print(f"\n⚠️  Reached API request limit of {args.max_requests}. Stopping.")
            break
            
        print(f"📍 Searching in: {city_name} (API calls: {total_api_calls}/{args.max_requests})")
        
        for keyword in SEARCH_KEYWORDS:
            # Check limit before each search
            total_api_calls = api_calls_nearby + api_calls_details
            if total_api_calls >= args.max_requests:
                break
                
            shops = search_nearby_shops(args.api_key, lat, lng, keyword)
            api_calls_nearby += 1
            total_found += len(shops)
            
            for place in shops:
                shop = parse_shop_from_place(place, args.api_key)
                
                # Check if exists
                if not args.dry_run and shop_exists(cursor, shop):
                    print(f"  ⊘ Already exists: {shop['name']}")
                    total_skipped += 1
                    continue
                
                if args.dry_run:
                    print(f"  ✓ Would insert: {shop['name']} - {shop['address']}")
                    total_inserted += 1
                else:
                    if insert_shop(cursor, shop):
                        print(f"  ✓ Inserted: {shop['name']} - {shop['address']}")
                        total_inserted += 1
                    else:
                        total_skipped += 1
            
            time.sleep(1)  # Rate limiting between keyword searches
    
    if connection:
        connection.commit()
        cursor.close()
        connection.close()
    
    print("\n" + "="*60)
    print("📊 SUMMARY")
    print("="*60)
    print(f"Total shops found: {total_found}")
    print(f"Total inserted: {total_inserted}")
    print(f"Total skipped: {total_skipped}")
    print(f"API Calls - Nearby Search: {api_calls_nearby}")
    print(f"API Calls - Place Details: {api_calls_details}")
    print(f"API Calls - Total: {api_calls_nearby + api_calls_details}/{args.max_requests}")
    print(f"Mode: {'DRY RUN' if args.dry_run else 'LIVE'}")
    print("="*60)


if __name__ == "__main__":
    main()
