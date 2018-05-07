
# fraud-detection
Proof of concept fraud detection based on Bayesian inference



## Running the app

Mongo DB local instance running on default port is required in order to run application.

Application is built using [Gradle](https://gradle.org/ ) build tool, therefore easiest way to run the application is running gradle run task in root application folder:

```
gradle run
```

## API

```
POST localhost:8080/evaluate-fraud/{transactionId}
```

JSON schema describing request body in:

```
 src/main/resources/api-schemas/request-schema.json
```

Example request is in:

```
additional-resources/example-request.json
```

API accepts details about debtor party, creditor party, transaction amount, transaction time and transaction location. Both parties are considered anonymous from fraud-detection point of view. External systems should provide details which allow to identify identify of party behind the transaction.  
