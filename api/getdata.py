from flask import Flask, jsonify, request
app = Flask(__name__)

@app.route('/', methods = ['GET','POST'])
def home():

    text = request.form["data"]
    print(text)
    return "received" 

if __name__ == "__main__":
    app.run(host="0.0.0.0")