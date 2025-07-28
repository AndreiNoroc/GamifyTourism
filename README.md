# GamifyTourism

GamifyTourism is an innovative project aimed at enhancing tourism experiences by integrating gamification elements. It combines mobile and wearable technology with beacon systems to create an engaging, interactive, and rewarding experience for users. The project is further detailed in the `FindNGo.pptx` presentation.

## Overview

Tourism can be more than just visiting places—it can be an adventure. GamifyTourism transforms traditional tourism into a game-like experience, encouraging exploration, interaction, and rewards. Users can track their visited locations, compete on leaderboards, earn vouchers, and receive location-based recommendations.

## Features

### Mobile App
- **Location Tracking**:
  - Automatically logs visited locations using GPS.
  - Displays a map with marked visited places.
- **Leaderboards**:
  - Compete with other users based on points earned.
  - View rankings by region, country, or globally.
- **Vouchers and Rewards**:
  - Earn vouchers for completing specific tasks or visiting certain locations.
  - Redeem rewards directly through the app.
- **Interactive Maps**:
  - Provides recommendations for nearby attractions.
  - Highlights locations with active tasks or rewards.
- **User Profiles**:
  - Track personal achievements and progress.
  - Customize profiles with avatars and badges.

### Watch App
- **Quick Access**:
  - View leaderboards, tasks, and notifications directly on the wearable device.
- **Task Notifications**:
  - Receive real-time updates about nearby tasks or rewards.
- **Activity Tracking**:
  - Monitor steps and integrate with gamification tasks.

### Beacon System
- **Bluetooth Beacons**:
  - Sends location-based signals to nearby devices.
  - Triggers app interactions such as unlocking tasks or rewards.
- **Seamless Integration**:
  - Works in real-time with the mobile app for a smooth user experience.

### Backend
- **API Endpoints**:
  - Provides robust support for app functionalities.
  - Handles user authentication, data storage, and task management.
- **Data Management**:
  - Stores user profiles, visited locations, and rewards securely.
  - Ensures scalability for a growing user base.

## Folder Structure

- `backend/`: Contains the server-side code and API endpoints.
- `Bluedroid_Beacon/`: Implements the beacon functionality using ESP-IDF.
- `Mobile_App/`: The Android mobile application with all the described features.
- `Watch_App/`: The Android wearable application with companion features.
- `tasks.txt`: A list of tasks and accomplishments for the project.
- `FindNGo.pptx`: A presentation describing the project in detail.

## Setup and Installation

### Backend
1. Navigate to the `backend/` folder.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the server:
   ```bash
   npm start
   ```

### Mobile App
1. Open the `Mobile_App/` folder in Android Studio.
2. Sync the Gradle files.
3. Build and run the app on an Android device or emulator.

### Watch App
1. Open the `Watch_App/` folder in Android Studio.
2. Sync the Gradle files.
3. Build and run the app on a compatible wearable device or emulator.

### Bluedroid Beacon
1. Navigate to the `Bluedroid_Beacon/` folder.

## How It Works

1. **User Registration**: Users sign up via the mobile app.
2. **Exploration**: Users visit locations equipped with Bluetooth beacons.
3. **Interaction**: Beacons send signals to the mobile app, triggering location-based tasks or rewards.
4. **Gamification**: Users earn points, climb leaderboards, and unlock vouchers.
5. **Companion Features**: The watch app provides quick access to essential features.

## Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes and push them to your fork.
4. Submit a pull request with a detailed description of your changes.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## Acknowledgments

- [ESP-IDF](https://github.com/espressif/esp-idf) for the beacon implementation.
- All contributors and collaborators who made this project possible: Hodolean Aurelian-Teofil, Cihan Ilhan, Noroc Andrei-Mihail.

