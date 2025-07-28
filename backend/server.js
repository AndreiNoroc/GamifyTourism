const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { MongoClient, ServerApiVersion } = require('mongodb');
import { GoogleGenAI } from "@google/genai";

const app = express();
const PORT = 8080;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(express.json());


// MongoDB URI
const uri = "mongodb+srv://user:passord@URI";

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

    const ai = new GoogleGenAI({ apiKey: "YOUR_API_KEY" });

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

        // Create the Gemini2.0 Flash prompt
        const prompt = `
        The user has visited the following locations: ${visitedLocations.join(', ')}.
        Based on these places, suggest ONE new travel destination they would love to visit.
        Make sure the recommendation matches their style or interests based on the visited places.
        Respond in 1-2 sentences.`;

        // Call Gemini API
        const completion = await ai.models.generateContent({
          model: "gemini-2.0-flash",
          contents: prompt,
        });

        const recommendation = completion.choices[0].message.content;

        res.status(200).json({ recommendation });
      } catch (error) {
        console.error('Error in /recommend-location:', error.response ? error.response.data : error.message);
        res.status(500).json({ error: 'Internal server error' });
      }
    });

    app.post('/get-visited-locations', async (req, res) => {
      try {
        const { username } = req.body;

        if (!username) {
          return res.status(400).json({ error: 'Username is required' });
        }

        // Căutăm userul după username
        const user = await usersCollection.findOne({ username });

        if (!user) {
          return res.status(404).json({ error: 'User not found' });
        }

        // Returnăm array-ul de visitedLocations
        const visitedLocations = user.visitedLocations || [];

        res.status(200).json({ visitedLocations });

      } catch (error) {
        console.error('❌ Error in /get-visited-locations:', error);
        res.status(500).json({ error: 'Internal server error' });
      }
    });

    app.post('/claim-voucher', async (req, res) => {
      try {
        const { username, voucherName } = req.body;
    
        if (!username || !voucherName) {
          return res.status(400).json({ error: 'Missing username or voucherName' });
        }
    
        // Find user
        const user = await usersCollection.findOne({ username });
        if (!user) {
          return res.status(404).json({ error: 'User not found' });
        }
    
        // Find voucher
        const voucher = await db.collection('tickets').findOne({ description: voucherName });
        if (!voucher) {
          return res.status(404).json({ error: 'Voucher not found' });
        }
    
        // Check user has enough points
        if ((user.score || 0) < voucher.points) {
          return res.status(400).json({ error: 'Not enough points' });
        }
    
        // Update user's score
        const newScore = (user.score || 0) - voucher.points;
        await usersCollection.updateOne({ username }, { $set: { score: newScore } });
    
        res.status(200).json({ message: 'Voucher claimed successfully' });
      } catch (error) {
        console.error('Error claiming voucher:', error);
        res.status(500).json({ error: 'Internal server error' });
      }
    });

    app.get('/challenges', async (req, res) => {
      try {
        const challenges = await db.collection('challenges')
          .find({}, { projection: { _id: 0, title: 1, points: 1 } })
          .toArray();
        res.status(200).json(challenges);
      } catch (error) {
        console.error('Error fetching challenges:', error);
        res.status(500).json({ error: 'Failed to fetch challenges' });
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