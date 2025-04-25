const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { MongoClient, ServerApiVersion } = require('mongodb');

const app = express();
const PORT = 8080;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// MongoDB URI
const uri = "mongodb+srv://andrei:andrei@cluster0.mgnrlde.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

// MongoClient setup
const client = new MongoClient(uri, {
  serverApi: {
    version: ServerApiVersion.v1,
    strict: true,
    deprecationErrors: true,
  }
});

async function startServer() {
  try {
    await client.connect();
    console.log("✅ Connected to MongoDB Atlas!");

    const db = client.db("smarttourism");
    const usersCollection = db.collection("users");
    const locationsCollection = db.collection("spots");

    // POST /register
    app.post('/register', async (req, res) => {
      const { username, password } = req.body;

      if (!username || !password) {
        return res.status(400).json({ message: 'Missing username or password' });
      }

      const existingUser = await usersCollection.findOne({ username });
      if (existingUser) {
        return res.status(409).json({ message: 'Username already exists' });
      }

      await usersCollection.insertOne({ username, password, "score": 0 });
      res.status(201).json({ message: 'User registered successfully' });
    });

    // POST /login
    app.post('/login', async (req, res) => {
      const { username, password } = req.body;

      if (!username || !password) {
        return res.status(400).json({ message: 'Missing credentials' });
      }

      const user = await usersCollection.findOne({ username, password });

      if (!user) {
        return res.status(401).json({ message: 'Invalid username or password' });
      }

      res.status(200).json({ message: 'Login successful' });
    });

    app.get('/score', async (req, res) => { 
      try { const { username } = req.query;
      if (!username) {
        return res.status(400).json({ error: 'Missing username' });
      }
      
      const user = await db.collection('users').findOne({ username });
      
      if (!user) {
        return res.status(404).json({ error: 'User not found' });
      }
      
      const score = user.score || 0;
      return res.status(200).json({ score });
    } catch (error) { console.error('Error in /score:', error); return res.status(500).json({ error: 'Internal server error' }); } });      

    app.get('/leaderboard', async (req, res) => {
      try {
        const users = await db.collection('users')
          .find({}, { projection: { _id: 0, username: 1, score: 1 } })
          .sort({ score: -1 })
          .toArray();
        res.json(users);
      } catch (error) {
        console.error("Error fetching leaderboard:", error);
        res.status(500).json({ message: "Internal server error" });
      }
    });

    app.get('/locations', async (req, res) => {
      try {
          const locations = await locationsCollection.find({}).toArray();
          res.json(locations);
      } catch (error) {
          console.error(error);
          res.status(500).json({ error: 'Failed to fetch locations' });
      }
    });

    app.get('/vouchers', async (req, res) => {
      try {
          const vouchers = await db.collection('vouchers').find().toArray();
          res.json(vouchers);
      } catch (err) {
          console.error(err);
          res.status(500).send('Server error');
      }
    });
    
    // Start listening
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`🚀 Server running at http://localhost:${PORT}`);
    });

  } catch (err) {
    console.error("❌ Error connecting to MongoDB:", err);
  }
}

startServer();