# TheVenerableInertia back-office system

-------------------------

## Overview

TVI back-office is a simple system that computes how much money vehicle drivers (aka EV drivers) owe TVI fo their charging sessions

#### Problem Statement :
- We should be able to submit a tariff by sending a POST request with JSON body
- The tariff (price) is compounded of three types of fees:
    * Energy consumed (expressed in currency / kWh, e.g. EUR / kWh)
    * Service fee (given as a number between 0.0 (exclusive) and 0.5 (inclusive))
    * Parking fee (expressed in currency / hour, e.g EUR / hour)
- Only one tariff can be active at any point in time
- Tariff cannot be applied retroactively
- Only tariff that starts in the future could be submitted
- We should be able to submit a charge session by sending a POST request
- A charge session cannot start nor end int the future
- We should be able to the overview by sending a GET request to an endpoint
- The overview should be provided in CSV format

#### Tech Stack Decision :

##### ZIO:

`ZIO` is a library for asynchronous and concurrent programming that is based on pure functional programming. Powered by highly-scalable, non-blocking fibers that never waste or leak resources, ZIO lets you build scalable, resilient, and reactive applications that meet the needs of your business. 
Unlike other effect libraries, most of the `ZIO` data types support three types parameters to define environment, error and the result type. Using environment type we can define the requirements of an effect and thanks to the Layers we can pass them easily to the effect. This helps us to perform dependency injection without techniques like `Cakce pattern` or using `implicits`.
ZIO has a very reach echosystem which makes it easy to use. It gives the ability to break down the problem to smaller problems, and then solve them individually. The beauty comes when you start composing those solutions into a new solution. The composition and abstraction gives you the ability to reuse code across your codebase and team members. That’s when you get efficiency and speed. It results in building higher quality software. It’s not only you are building quicker, you build high quality software as well.

##### http4s

* *composible*: `http4s` takes advantage of cats to achieve path `Matcher`. This feature makes it easier to not only compose routes but also it helps to add more directives like authorization and authentication to the routes
* *extensible*: Since route matcher is simply just `Kleisli`, extending http4s to support types other than `F[Response[F]]` will be much simpler. For example in this application it's extended to use `AppTask[A]` which is a type alias for `RIO[AppEnvironment, A]`

##### refined

`refined` has been used to add more constrains on the data types. Along with the refined library, some validations and custom Circe encoders and decoders are added to the project to validate the input data and read them with the desired format.

##### Circe

`Circe` is a JSON library for Scala. It's fully compatible with `http4s` and the auto derivation feature makes creating the type class instances easier.
Under the hood circe is implemented using `Shapeless` and `macros` which adds more overhead during the compile time. As an alternative I could use `zio-json` which is implemented using `Magnolia` but it's not fully released.

##### zio-config

`zio-config` is able to read the configurations from different kind of sources including Hocon files, Environment variables, YAML files, etc. Since the parsed configurations are provided as a layer, accessing the configurations from different layers of the application is very easy and straightforward.

##### zio-magic

Layers are a great feature of ZIO but sometimes providing layers could be a bit difficult. `zio-magic` facilitates providing the layers but automatically resolving the dependencies between different layers and wiring them together.

Note: A simple authentication mechanism has been added to the project to demonstrate the idea of limiting the access to the restricted endpoints 

## How to run

### Prerequisites
The Project uses below tool versions :

|API name|Version|
|---|---|
|JDK|1.8+|
|Scala|2.13|
|Sbt|1.0+|
|Docker Compose (Optional)|3.0+|

### Run the application using SBT

Since there is only one executable application in the project, it's suffice to run the following command to run the application:

```shell
sbt run
```

### Run the application using docker-compose
As an alternative to running the application using SBT, it's also possible to make a Docker images out of the project and run it using Docker or docker-compose:

#### Create Docker image
Run the following command to create a Docker image out of the project artifacts:

```shell
sbt docker:publishLocal
```

#### Run TVI back-office 
Using `docker-compose` we can run the Docker images:

```shell
docker-compose up -d
```

note: this command needs to be run from the root directory of the project in which the `docker-compose.yml` file exists. As another option the file can be passed to the command via `-f`:
```shell
docker-compose -f <path-to-docker-compose.yml> up
```

### Stop container
Use the following command to stop the running container:

```shell
docker-compose down -v
```

### Test the application

In order to simplify testing the application a simple script has been added to the project which sends the required requests to add a tariff and a couple of charge sessions.
At the end script also makes a call to the server to retriew the overview. Please sun the following script to test the application:

```shell
./script/load.sh
```

#### Synchronize timezone from host to container

Note: On MacOS the time might need to synchronize timezone from host to container otherwise you may see the following error:

```json
{"error":"A charge session can't start nor end in the future"}
```

As a workaround you can run the container by passing the timezone as an environment variable:

```shell
export TZ=$(readlink /etc/localtime | sed 's#/var/db/timezone/zoneinfo/##');
docker run -e "TZ=${TZ}" -p 8181:8181 tvi/back-office:0.1.0
```

### Make HTTP call
If you want to manually send the HTTP request please use the following commands:

#### Submit a tariff

```shell
curl -H "Content-Type: application/json" \
-d '{"fee":"1.0 EUR/kWh","parkingFee":"0.01 EUR/hour","serviceFee":0.02}' \
-u username:password http://localhost:8181/tariff
```
Note: The endpoint is accessible by authorized users

You can also specify a start date for a tariff (we are only able to submit a tariff that starts in the future)

```shell
curl -H "Content-Type: application/json" \
-d '{"startsFrom":"2021-04-10T23:20:50Z","fee":"1.0 EUR/kWh","parkingFee":"0.01 EUR/hour","serviceFee":0.02}' \
-u username:password http://localhost:8181/tariff
```

#### Submit a charge session

```shell
curl -H "Content-Type: application/json" \
-d '{"driverId":"driver-1","sessionStartTime":"2021-04-12T00:00:20Z","sessionEndTime":"2021-04-12T00:00:22Z","consumedEnergy":"100.0 kWh"}' \
http://localhost:8181/session
```

Note: A Charge session cannot start nor end in the future and there should be an active tariff that starts before `sessionStartTime`.

#### Download the overview file

Run the following command to download an overview as a CSV file:

```shell
curl -H "Content-Type: application/json" \
http://localhost:8181/session/driver-1
```

### Run tests
```shell
sbt test
```