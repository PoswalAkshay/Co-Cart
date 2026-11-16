# Co-Cart 🛒
> **Real-Time Collaborative Household Grocery Checklist Application for Android**

Co-Cart is a lightweight, cloud-synchronized grocery checklist app designed for roommates, hostels, and shared flats. It eliminates duplicate purchases, forgotten supplies, and cluttered messaging threads by keeping household members connected in real time on a single, shared checklist.

---

## 🌟 Key Features

### 🚀 1. Onboarding & Authentication
- **Animated Splash Screen**: Smooth 1.2-second scale & fade-in branding animation (`ObjectAnimator`).
- **Personalized Welcome Screen**: Greets returning users (*"Welcome back, [User Name]!"*) with feature cards and a prominent **`Get Into App 🚀`** button.
- **Secure Auth**: Email/Password login and registration with persistent auto-login sessions.

### 👤 2. Profile Management
- **Structured Name Fields**: First, Middle, and Last Name.
- **Gallery Photo Upload**: Circular avatar with an interactive camera badge to choose profile pictures.
- **Country Code Dropdown**: Dropdown list featuring country names, flag emojis, and codes (`🇮🇳 India (+91)`, `🇺🇸 USA (+1)`, `🇬🇧 UK (+44)`, `🇨🇦 Canada (+1)`, etc.).
- **Strict Mobile Validation**: Enforces 10-digit phone formatting (`0-9`).
- **Demographics**: Optional Age, Gender Choice Chips (`Male 👨`, `Female 👩`, `Other ⚧`), and Date of Birth calendar picker.

### 🏠 3. Household Management & Host Approval Security
- **Room Creation**: Generates a 6-character room invite code (e.g., `A9EF46`).
- **Host Approval System**: Roommates enter a code ➔ Host receives a real-time red notification badge (`"1 Pending Join Request 🔔"`) ➔ Host accepts or rejects ➔ Roommate automatically enters the room upon acceptance.
- **Code Regeneration**: Host can refresh the 6-character invite code anytime.
- **Room Switching & Sharing**: Switch rooms anytime or share invite links via WhatsApp / Native Share Sheet.

### 🛒 4. Smart Shared Grocery Checklist
- **Real-Time Cloud Synchronization**: Built on Firebase Cloud Firestore (`addSnapshotListener`) for real-time updates across devices.
- **Supermarket Aisle Auto-Sorting**: Automatically categorizes and orders items by store aisle (`Dairy 🥛` ➔ `Produce 🥦` ➔ `Bakery 🍞` ➔ `Snacks 🍿` ➔ `Drinks 🥤` ➔ `General 🛒`).
- **Interactive Swipe Gestures (`ItemTouchHelper`)**:
  - **Swipe Right**: Toggles item purchased status (strike-through text + green status badge).
  - **Swipe Left**: Deletes item from checklist.
- **Quick Quantity Presets**: 1-tap quantity chips (`1 Ltr`, `1 kg`, `1 Pack`, `500g`, `2 pcs`).
- **🚨 Urgent Item Tags**: Highlight critical supplies with a red urgent badge.

### 💸 5. Live Cart Cost Tracking & Settlement
- **Live Price Breakdown**: Displays `Total Cost` (sum of all items) and `Purchased Cost` (sum of checked items).
- **Clear Purchased Items 🧹**: 1-tap confirmation dialog to clear checked-off items in real time.

---

## 🎨 Design System ("Fresh Market" Palette)

- **Primary Color (`#2E7D32`)**: Fresh Dark Green for Top Toolbar, Header Card, and FAB.
- **Accent Color (`#FFB300`)**: Amber/Gold for Progress Bar, Checkboxes, and Profile Buttons.
- **Background (`#F1F8E9`)**: Soft light green background tint.
- **Card Styling**: Material Design 3 cards with 16dp–20dp rounded corners.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Java (JDK 11) |
| **Target SDK** | Android 15 / API Level 35 (Min API 24) |
| **UI Components** | AndroidX Material Design 3 |
| **Cloud Database** | Firebase Cloud Firestore (NoSQL) |
| **Authentication** | Firebase Authentication |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |
| **Image Loading** | Glide (`com.github.bumptech.glide`) |
| **Animations** | Airbnb Lottie (`com.airbnb.android:lottie`) |
| **Offline Cache** | SharedPreferences + JSON Serialization |

---

## 🚀 Getting Started & Setup

### Prerequisites
- Android Studio Ladybug / 2024.2.2 or newer
- JDK 11
- Android device or emulator running API Level 24+

### Installation Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/PoswalAkshay/Co-Cart.git
   cd Co-Cart
   ```

2. **Configure Firebase**:
   - Create a project on [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App with package name `com.example.co_cart`.
   - Download `google-services.json` and place it in the `app/` directory (see `app/google-services.json.template` for reference).
   - Enable **Firebase Authentication** (Email/Password) and **Cloud Firestore Database**.

3. **Build & Run**:
   - Open the project in Android Studio.
   - Sync Gradle project files.
   - Click **Run** ▶️ to install on your emulator/device.

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
