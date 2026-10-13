# 🌦️ SkyCast – Real-Time Weather Application Using Java Stream API

SkyCast is a web-based weather application built using Java that displays real-time weather information and a five-day forecast for any supported city. It uses the OpenWeatherMap API to retrieve weather data and Java Stream API concepts to process forecast information.

## ✨ Features

* 🌍 **City-Based Weather Search** – Search for weather information by city name.
* 🌡️ **Current Weather** – View temperature, feels-like temperature, humidity, wind speed, and atmospheric pressure.
* 🕒 **Today's Forecast** – View available three-hour temperature forecasts.
* 📅 **Five-Day Forecast** – View daily average temperatures and the highest forecast rain probability for each day.
* ☁️ **Weather Icons** – Display weather conditions with corresponding icons.
* 🤖 **Gemini AI Weather Assistant** – Ask weather-related questions using Google's Gemini API, when configured.
* 📱 **Responsive Interface** – Dark-themed dashboard designed for desktop and mobile screens.
* 🔐 **Environment-Based API Keys** – Keep API keys outside the source code.

## 🛠️ Technologies Used

* Java 21
* Java Stream API
* Java HTTP Client
* Java `HttpServer`
* HTML5
* CSS3
* JavaScript
* OpenWeatherMap API
* Google Gemini API (optional AI assistant)
* Visual Studio Code
* Git and GitHub

## 📋 Prerequisites

Before running the application, make sure you have:

* Java Development Kit (JDK) 21 or compatible version
* Visual Studio Code or another Java-compatible editor
* An OpenWeatherMap API key
* A Google Gemini API key if you want to use the AI assistant
* Internet access

**Note:** Maven is not required for the current version of this project.

## 📁 Project Structure

```text
java-mini-project/
├── src/
│   ├── Main.java
│   ├── Weather.java
│   ├── WeatherService.java
│   ├── Forecast.java
│   ├── WeatherWebServer.java
│   ├── GeminiChat.java
│   └── index.html
└── README.md
```

The exact files may vary depending on your current implementation.

## 🔑 API Configuration

### 1. OpenWeatherMap API

Obtain an API key from [OpenWeatherMap](https://openweathermap.org/api).

Set the environment variable in PowerShell:

```powershell
$env:OPENWEATHER_API_KEY = "YOUR_OPENWEATHER_API_KEY"
```

### 2. Google Gemini API (Optional)

Obtain a Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey).

Set the environment variable:

```powershell
$env:GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"
```

Replace the placeholder values with your actual keys. Never commit API keys to GitHub.

These PowerShell variables apply to the current terminal session. If you open a new terminal, configure them again.

## ▶️ How to Run the Project

### Step 1: Clone the Repository

```powershell
git clone https://github.com/balajiads/java-mini-project.git
```

### Step 2: Open the Project

```powershell
cd java-mini-project
```

Open this folder in Visual Studio Code.

### Step 3: Configure API Keys

Set the OpenWeatherMap API key and, if needed, the Gemini API key in the same PowerShell terminal that will run the application.

### Step 4: Compile the Java Files

```powershell
javac -encoding UTF-8 --add-modules jdk.httpserver -cp src -d src src\*.java
```

### Step 5: Start the Web Server

```powershell
java --add-modules jdk.httpserver -cp src WeatherWebServer
```

If the server starts successfully, the terminal should display a message similar to:

```text
Weather website running at http://localhost:8080
Keep this terminal open.
```

### Step 6: Open the Application

Open your browser and visit:

**http://localhost:8080**

Enter a city name, click Search, and view the available weather information and forecast.

## 🌊 How Java Stream API Is Used

Java Stream API can simplify the processing and aggregation of forecast data.

Typical operations include:

* `filter()` – Select forecast entries for a particular date.
* `map()` – Extract temperatures or other required values.
* `collect()` – Group forecast entries by date.
* `reduce()` – Calculate a total or average from temperature values.
* `sorted()` – Arrange forecast entries chronologically.

These operations help make forecast processing more readable and maintainable. The exact operations used depend on the implementation in the Java source files.

## 🤖 Gemini AI Weather Assistant

When configured, the Gemini AI assistant accepts a user's question, retrieves the available weather context, and sends a request to Google's Gemini API.

For example:

* What is the current temperature in Chennai?
* How humid is the weather?
* What is the forecast for the next few days?
* Is rain expected in the available forecast?

The assistant should use the supplied weather data and avoid inventing conditions when the data is unavailable.

## ⚠️ Troubleshooting

### Port 8080 Is Already in Use

Check the process using port 8080:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

Check the process ID shown under `OwningProcess`:

```powershell
Get-Process -Id YOUR_PID
```

If you confirm it is your previous Java weather server, stop it:

```powershell
Stop-Process -Id YOUR_PID -Force
```

Then start the application again.

### Weather Data Is Not Loading

* Verify that `OPENWEATHER_API_KEY` is configured.
* Check your internet connection.
* Check the server terminal for error messages.
* Confirm that the OpenWeatherMap API key has access to the requested endpoints.
* Test the weather endpoint directly:

```text
http://localhost:8080/api/weather?city=Chennai
```

### Gemini Assistant Is Not Responding

* Verify that `GEMINI_API_KEY` is configured.
* Check the server terminal for Gemini API errors.
* Verify model access, API quota, and network connectivity.

## 🔒 Security

* Do not hardcode API keys in Java or HTML files.
* Do not upload API keys to GitHub.
* Use environment variables to configure credentials.
* If a key is accidentally exposed, revoke it and generate a replacement.

## 🚀 Future Enhancements

* Automatic location-based weather
* Weather alerts and notifications
* Weather data visualization and charts
* Unit conversion between Celsius and Fahrenheit
* Improved AI assistant conversation history
* More detailed forecast analytics
* Automated tests and build configuration

## 👨‍💻 Author

**Balaji**

GitHub: [balajiads](https://github.com/balajiads)

## 📄 License

This project is intended for learning and educational purposes. Add a license file if you want to specify the terms under which others may use or distribute the project.
