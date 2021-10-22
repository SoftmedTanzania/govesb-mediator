# GOVESB Mediator

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4ffde7850d3640dda7efabf921a27be5)](https://app.codacy.com/gh/SoftmedTanzania/govesb-mediator?utm_source=github.com&utm_medium=referral&utm_content=SoftmedTanzania/govesb-mediator&utm_campaign=Badge_Grade_Settings)
[![Java CI Badge](https://github.com/SoftmedTanzania/govesb-mediator/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/SoftmedTanzania/govesb-mediator/actions?query=workflow%3A%22Java+CI+with+Maven%22)
[![Coverage Status](https://coveralls.io/repos/github/SoftmedTanzania/govesb-mediator/badge.svg?branch=development)](https://coveralls.io/github/SoftmedTanzania/govesb-mediator?branch=development)

An [OpenHIM](http://openhim.org/) mediator for handling system integration with destination systems via Government of
Tanzania Enterprise Service Bus (GovESB).

## 1. Dev Requirements

1. Java 1.8
2. IntelliJ or Visual Studio Code
3. Maven 3.6.3

## 2. Mediator Configuration

This mediator is designed to work with multiple systems that send JSON Payloads while communicating with the NHCR via
the HIM and the HIM transforms the messages into HL7v2 messages before forwarding the requests to the NHCR

### 3 Configuration Parameters

The configuration parameters specific to the mediator and destination system can be found at

`src/main/resources/mediator.properties`

```
    # Mediator Properties
    mediator.name=GOVESB-Mediator
    mediator.host=localhost
    mediator.port=3105
    mediator.timeout=600000
    mediator.heartbeats=true

    core.host=localhost
    core.api.port=8080
    core.api.user=root@openhim.org
    core.api.password=openhim-password

    #GOVESB Configurations.
    govesb.client.id=client.id
    govesb.client-secret=client-secret
    govesb.client.accessTokenUri=accessTokenUri
    govesb.user.id=id
    govesb.uri=uri
    govesb.apiCode=apiCode

    #SYSTEM Configurations.
    system.private-key=private-key
    system.public-key=public-key
```

The configuration parameters specific to the mediator and the mediator's metadata can be found at

`src/main/resources/mediator-registration-info.json`

```
{
  "urn": "urn:uuid:6f220930-3265-11ec-9f31-6b29653de558",
  "version": "0.1.0",
  "name": "GOVESB Mediator",
  "description": "An openHIM mediator for handling system integration between source systems via the Tanzania HIM and GOVESB",
  "endpoints": [
    {
      "name": "GOVESB Mediator Route",
      "host": "localhost",
      "port": "3105",
      "path": "/govesb",
      "type": "http"
    }
  ],
  "defaultChannelConfig": [
    {
      "name": "GOVESB Mediator",
      "urlPattern": "^/govesb$",
      "type": "http",
      "allow": ["govesbmediator"],
      "routes": [
        {
          "name": "GOVESB Mediator Route",
          "host": "localhost",
          "port": "3105",
          "path": "/govesb",
          "type": "http",
          "primary": "true"
        }
      ]
    }
  ],
  "configDefs": [
    {
      "param": "govesbProperties",
      "displayName": "Government Enterprise Service Bus Properties",
      "description": "Configuration to set the client id, client-secret, access token uri, user-id, uri, privateKey and apiCode for authentication and communication with GOVESB",
      "type": "struct",
      "template": [
        {
          "param": "userId",
          "displayName": "TanzaniHIM GOVESB's User Id",
          "description": "TanzaniaHIM GOVESB's User id",
          "type": "string"
        },
        {
          "param": "clientId",
          "displayName": "TanzaniHIM GOVESB's Client ID",
          "description": "TanzaniHIM GOVESB's client id",
          "type": "string"
        },
        {
          "param": "clientSecret",
          "displayName": "Client Secret",
          "description": "Client Secret obtained in GOVESB",
          "type": "password"
        },
        {
          "param": "accessTokenUri",
          "displayName": "Access Token URI",
          "description": "The access token URI",
          "type": "string"
        },
        {
          "param": "govEsbUri",
          "displayName": "GOVESB URI",
          "description": "The URI for accessing GOVESB",
          "type": "string"
        },
        {
          "param": "govEsbApiCode",
          "displayName": "GOVESB API Code",
          "description": "The API Code",
          "type": "string"
        },
        {
          "param": "privateKey",
          "displayName": "The System's Private Key",
          "description": "The System's Private Key used while authenticating with GOVESB",
          "type": "password"
        }
      ]
    }
  ]
}

```

## 4. Deployment

To build and run the mediator after performing the above configurations, run the following

```
  mvn clean package -DskipTests=true -e source:jar javadoc:jar
  java -jar target/govesb-mediator-<version>-jar-with-dependencies.jar
```
