from flask import Flask, jsonify, request
from recommendation import getRecommendation  # Import your recommendation function
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Endpoint to get recommendations for a customer
@app.route('/recommendations/<int:id>', methods=['GET'])
def get_recommendations(id):
    try:
        # Get recommendations for the customer
        recommendations = getRecommendation(id)

        # If recommendations are found, return them as a JSON response
        if recommendations:
            return jsonify({
                'status': 'success',
                'customer_id': id,
                'recommended_products': recommendations
            })
        else:
            return jsonify({
                'status': 'error',
                'message': 'No recommendations found for this customer.'
            }), 404
    except Exception as e:
        print (e)
        # Handle any errors during the process
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500

if __name__ == '__main__':
    # Run the app in debug mode (for development purposes)
    app.run(debug=True)
