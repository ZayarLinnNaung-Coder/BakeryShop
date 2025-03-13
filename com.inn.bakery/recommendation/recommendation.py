import pandas as pd
from sklearn.preprocessing import OneHotEncoder, MinMaxScaler
from sklearn.metrics.pairwise import cosine_similarity
from sqlalchemy import create_engine

# Set up connection parameters
host = 'localhost'
database = 'bakeryshop'
user = 'root'
password = '1234'

# Establish SQLAlchemy connection
connection_string = f'mysql+mysqlconnector://{user}:{password}@{host}/{database}'

def getRecommendation(id):
    print("gotID:", id)

    engine = create_engine(connection_string)

    ### Step 1: Extract Product Data
    product_query = """
        SELECT p.id, p.name, c.id As categoryId, c.name AS category, p.flavor, p.price
        FROM product p
        JOIN category c ON p.category_fk = c.id;
    """
    product_df = pd.read_sql_query(product_query, engine)

    ### Step 2: Extract User Purchase History
    purchase_query = """
    SELECT
        u.id AS customer_id,
        b.name AS customer_name,
        p.id AS product_id,
        jt.product_name,
        jt.product_category AS category,
        p.flavor,
        jt.price
    FROM bill b
    JOIN JSON_TABLE(b.productdetails, '$[*]'
        COLUMNS (
            product_name VARCHAR(255) PATH '$.Name',
            product_category VARCHAR(255) PATH '$.Category',
            price DECIMAL(10,2) PATH '$.Price'
        )
    ) AS jt
    JOIN product p ON jt.product_name = p.name
    JOIN user u ON u.email= b.email
    ORDER BY b.name, jt.product_name;
    """
    df_bill = pd.read_sql_query(purchase_query, engine)

    # Close database connection
    engine.dispose()

    # Convert user purchase history into a dictionary {user_id: [product_names]}
    user_purchase_history = df_bill.groupby('customer_id')['product_name'].apply(list).to_dict()

    ### Step 3: Feature Encoding (Handling Multiple Flavors)
    encoder = OneHotEncoder(sparse_output=False, handle_unknown='ignore')

    # One-hot encode `category`
    category_encoded = encoder.fit_transform(product_df[['category']])
    category_df = pd.DataFrame(category_encoded, columns=encoder.get_feature_names_out(['category']))

    # One-hot encode `flavor` (split multi-flavored products)
    flavor_encoded = product_df['flavor'].str.get_dummies(sep=', ')
    flavor_df = pd.DataFrame(flavor_encoded)

    # Normalize price
    scaler = MinMaxScaler()
    product_df['normalized_price'] = scaler.fit_transform(product_df[['price']])

    # Combine encoded category, flavor, and price into a feature matrix
    product_features = pd.concat([product_df[['id']], category_df, flavor_df, product_df[['normalized_price']]], axis=1)

    ### Step 4: Compute User Profiles
    user_profiles = {}

    for user, purchased_products in user_purchase_history.items():
        # Extract features for purchased products
        purchased_features = product_features[product_features['id'].isin(
            product_df[product_df['name'].isin(purchased_products)]['id']
        )].drop(columns=['id'])

        # Compute the average feature vector
        if not purchased_features.empty:
            user_profiles[user] = purchased_features.mean(axis=0)

    ### Step 5: Compute Cosine Similarity and Generate Recommendations
    recommendations = {}

    for user, user_profile in user_profiles.items():
        # Compute cosine similarity between the user profile and all products
        feature_matrix = product_features.drop(columns=['id']).values
        user_similarity = cosine_similarity([user_profile], feature_matrix)[0]  # Compare user profile with all products

        # Create a Series with product_id as index and similarity as values
        similar_products = pd.Series(user_similarity, index=product_df['id'])

        # Remove products the user has already purchased
        purchased_product_ids = product_df[product_df['name'].isin(user_purchase_history[user])]['id']
        similar_products = similar_products.drop(labels=purchased_product_ids, errors='ignore')

        # Get top 5 recommendations
        recommended_products = similar_products.sort_values(ascending=False).head(5).index.tolist()
        recommendations[user] = recommended_products

    ### Step 6: Return Recommended Products for the Given User ID
    if id not in recommendations:
        return []

    recs = recommendations[id]
    recommend = []

    for product_id in recs:
        # Get product details
        product_info = product_df[product_df['id'] == product_id][['name', 'categoryId', 'category', 'flavor']]
        product_name = product_info['name'].values[0]
        category_id = int(product_info['categoryId'].values[0])
        category_name = product_info['category'].values[0]
        flavor_name = product_info['flavor'].values[0]
        recommend.append({"id": product_id, "name": product_name, "category_id": category_id, "category": category_name, "flavor": flavor_name})

    return recommend
