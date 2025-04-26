const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { MongoClient, ServerApiVersion } = require('mongodb');
const OpenAI = require('openai');

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
          const vouchers = await db.collection('tickets').find().toArray();
          res.json(vouchers);
      } catch (err) {
          console.error(err);
          res.status(500).send('Server error');
      }
    });

    // POST /visit-location
    app.post('/visit-location', async (req, res) => {
      try {
        const { username, locationName } = req.body;

        if (!username || !locationName) {
          return res.status(400).json({ error: 'Missing username or locationName' });
        }

        // Find user
        const user = await usersCollection.findOne({ username });
        if (!user) {
          return res.status(404).json({ error: 'User not found' });
        }

        // Find location
        const location = await locationsCollection.findOne({ name: locationName });
        if (!location) {
          return res.status(404).json({ error: 'Location not found' });
        }

        // Check if location was already visited
        const visitedLocations = user.visitedLocations || [];
        if (visitedLocations.includes(locationName)) {
          return res.status(400).json({ error: 'Location already visited' });
        }

        // Update user's score
        const newUserScore = (user.score || 0) + (location.score || 0);

        // Add location to visitedLocations
        visitedLocations.push(locationName);

        await usersCollection.updateOne(
          { username },
          {
            $set: {
              score: newUserScore,
              visitedLocations: visitedLocations
            }
          }
        );

        // Update location's score (decrease by 1, minimum 0)
        const newLocationScore = Math.max((location.score || 0) - 1, 0);
        await locationsCollection.updateOne(
          { name: locationName },
          { $set: { score: newLocationScore } }
        );

        res.status(200).json({ 
          message: 'Score and visited locations updated successfully', 
          newUserScore, 
          newLocationScore,
          updatedVisitedLocations: visitedLocations
        });
      } catch (error) {
        console.error('Error in /visit-location:', error);
        res.status(500).json({ error: 'Internal server error' });
      }
    });

    const openai = new OpenAI({
      apiKey: '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',
    });

    // POST /recommend-location
    app.post('/recommend-location', async (req, res) => {
      try {
        const { username } = req.body;

        if (!username) {
          return res.status(400).json({ error: 'Missing username' });
        }

        // Find user
        const user = await usersCollection.findOne({ username });
        if (!user) {
          return res.status(404).json({ error: 'User not found' });
        }

        // Get user's visited locations
        const visitedLocations = user.visitedLocations || [];

        if (visitedLocations.length === 0) {
          return res.status(400).json({ error: 'No visited locations found for this user' });
        }

        // Create the OpenAI prompt
        const prompt = `
        The user has visited the following locations: ${visitedLocations.join(', ')}.
        Based on these places, suggest ONE new travel destination they would love to visit.
        Make sure the recommendation matches their style or interests based on the visited places.
        Respond in 1-2 sentences.`;

        // Call OpenAI API
        const completion = await openai.chat.completions.create({
          model: 'gpt-3.5-turbo',
          messages: [
            { role: 'system', content: 'You are a helpful travel assistant.' },
            { role: 'user', content: prompt }
          ],
          max_tokens: 150
        });

        const recommendation = completion.data.choices[0].message.content.trim();

        res.status(200).json({ recommendation });
      } catch (error) {
        console.error('Error in /recommend-location:', error.response ? error.response.data : error.message);
        res.status(500).json({ error: 'Internal server error' });
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