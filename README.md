# WhosInBot Scala

[![Build Status](https://travis-ci.org/tonylpt/WhosInBot-Scala.svg?branch=master)](https://travis-ci.org/tonylpt/WhosInBot-Scala)

This is a rewrite of the [WhosInBot](https://github.com/col/whos_in_bot) in Scala. 
It serves as a playground for the Scala language and ecosystem, including Akka and Slick.
The choice of technologies for this project is more experimental than pragmatic. 

Check out the Clojure version [here](https://github.com/tonylpt/WhosInBot-Clojure).

  
## Usage
Refer to the original [WhosInBot](https://github.com/col/whos_in_bot/blob/master/README.md) for the full usage description.

### Basic Commands
- `/start_roll_call` - Start a new roll call
- `/start_roll_call Some cool title` - Start a new roll call with a title
- `/set_title Some cool title` - Add a title to the current roll call
- `/end_roll_call` - End the current roll call

### Attendance Commands
- `/in` - Let everyone know you'll be attending
- `/in Some random comment` - Let everyone know you'll be attending, with a comment
- `/out` - Let everyone know you won't be attending
- `/out Some excuses` - Let everyone know you won't be attending, with a comment
- `/maybe` - Let everyone know that you might be coming
- `/maybe Erm..` - Let everyone know that you might be coming, with a comment
- `/set_in_for Dave` - Let everyone know that Dave will be attending (with an optional comment)
- `/set_out_for Dave` - Let everyone know that Dave won't be attending (with an optional comment)
- `/set_maybe_for Dave` - Let everyone know that Dave might be coming (with an optional comment)
- `/whos_in` - List attendees

### Other Commands
- `/shh` - Tells WhosInBot not to list all attendees after every response
- `/louder` - Tells WhosInBot to list all attendees after every response


## Development

### Prerequisites
- [SBT](https://www.scala-sbt.org/1.0/docs/Setup.html)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Setup
1. [Create a Telegram bot](https://core.telegram.org/bots#creating-a-new-bot) for development and obtain the authorization token.
2. Copy `src/main/resources/application.template.conf` to `src/main/resources/application.conf` and fill in the Telegram token.        
3. Start the development PostgreSQL and Redis with Docker Compose:

        docker-compose up -d
        
   This automatically creates `whosin_dev` and `whosin_test` databases.
   
### Run with SBT
1. Apply dev database migrations:

        sbt flywayMigrate
        
2. Apply test database migrations:

        sbt test:flywayMigrate
        
3. Run tests:

        sbt test
        
4. Run the app locally:

        sbt run
        

### Run from JAR
1. Make sure `src/main/resources/application.conf` has the correct values.
2. Make sure the test database is running with migrations applied:

        sbt test:flywayMigrate
        
3. Build the JAR:

        sbt assembly

    This also runs unit tests by default. The standalone JAR will be generated at `target/scala-2.12/WhosInBot-Scala.jar`.
    
4. Apply database migrations:

        sbt flywayMigrate
        
5. Run the app:        
       
        java -jar target/scala-2.12/WhosInBot-Scala.jar
        
        