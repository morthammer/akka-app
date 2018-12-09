# AKKA HTTP Weather Fetching App

Simple Akka Http application to fetch weather for city/state and recall the search with a cookie stored on the client side

## Getting Started

The environment is assumed to be *nix

### Prerequisites

```
sbt 0.13
java 1.8
scala 2.12
```

### Installing

Clone project from git

```
git clone https://github.com/morthammer/akka-app.git
```

change directory into akka-app

```
cd akka-app
```

run the project with sbt

```
sbt clean run
```

The main functionality of the app is exposed file GeoPlotRoute which has 2 endpoints, a get and a post to http://localhost:8080/plot

Here are some example api calls:

```
curl -X POST \
  http://localhost:8080/plot \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{
	"geoInputPoints": [
	{"city": "Seattle","state": "WA"},
	{"city": "Miami","state": "FL"}
	]
      }'
```

```
curl -v --cookie "weathertracker={your_token_value_from_above}" -X GET http://localhost:8080/plot
```

## Running the tests

The unit tests can be run from the root akka-app directory using sbt with the command

```
sbt test
```
