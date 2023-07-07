# XfccMediator

The `XfccMediator` is a custom mediator for WSO2 Micro Integrator that performs validation of client certificates based on X.509 certificates.

## Table of Contents
- [XfccMediator](#xfccmediator)
  - [Table of Contents](#table-of-contents)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Usage](#usage)
  - [Environment Variables](#environment-variables)
  - [Error Handling](#error-handling)
    - [Exceptions](#exceptions)
  - [Contributing](#contributing)
  - [License](#license)

## Prerequisites

To use the `XfccMediator`, you need to have the following prerequisites:

1. WSO2 Micro Integrator installed and running.
2. Java Development Kit (JDK) installed on your system.

## Installation

### With pom.xml

Add the following dependency to your pom.xml
```xml
<dependency>
  <groupId>io.integon</groupId>
  <artifactId>wso2-mi-xfcc-mediator</artifactId>
  <version>1.0.1</version>
</dependency>
```

These dependencies are required for the wso2-mi-xfcc-mediator to work. 

### Without pom.xml

Add the wso2-mi-xfcc-mediator jar to the MI Folder "/home/wso2carbon/wso2mi-{version}/lib"


the .jar artifact is available on the maven central repository [here](https://s01.oss.sonatype.org/service/local/repositories/releases/content/io/integon/wso2-mi-xfcc-mediator/1.0.1/wso2-mi-xfcc-mediator-1.0.1.jar) or as a release artefact on GitHub [here](https://github.com/integon/wso2-mi-xfcc-mediator/releases)

## Configuration

To configure the `XfccMediator`, follow these steps:

1. Set the required environment variables mentioned in the [Environment Variables](#environment-variables) section.
2. Add the `XfccMediator` to your integration sequence or endpoint configuration in the WSO2 Micro Integrator.

## Usage

To use the `XfccMediator`, you need to include it in your integration sequence or endpoint configuration within the WSO2 Micro Integrator. Here's an example of how to use it:

```xml
<?xml version="1.0" encoding="uTF-8"?>
<api context="/validate" name="validate_xfcc" xmlns="http://ws.apache.org/ns/synapse" trace="enable" statistics="enable">
    <resource methods="GET" uri-template="/">
        <inSequence>
            <class name="ch.integon.XfccMediator" />  
        <!-- Your sequence  -->    
        </inSequence>
    </resource>
</api>
```
When a request passes through the XfccMediator, it performs the following steps:

1. Retrieves the client certificate from the specified HTTP header.
2. Validates the certificate against the configured truststore.
3. Validates the common name (CN) of the certificate against the allowed CNs.
4. If any validation step fails, the mediator throws a SynapseException with an appropriate error message, which can be handled by the error handling mechanism in WSO2 Micro Integrator.

## Environment Variables

| Variable Name       | Description                                                                             | Default Value |
| ------------------- | --------------------------------------------------------------------------------------- | ------------- |
| TRUSTSTORE_PATH     | Specifies the path to the truststore file containing the trusted CA certificates.       | -             |
| TRUSTSTORE_PASSWORD | Specifies the password to access the truststore.                                        | -             |
| HEADER_NAME         | Specifies the name of the HTTP header that contains the client certificate.             | X-Client-Cert |
| ALLOWED_CNS         | Specifies a comma-separated list of allowed common names (CNs) for client certificates. | -             |

## Error Handling

The XfccMediator provides the following error codes for error handling

| Error Code | Description                         |
| ---------- | ----------------------------------- |
| 401        | Invalid client certificate.         |
| 401        | Certificate validation failed.      |
| 403        | Common name (CN) validation failed. |
| 500        | Unexpected exception                |


### Exceptions

| Exception             | Description                                |
| --------------------- | ------------------------------------------ |
| SynapseException      | Failure occurring during mediation process |
| IllegalStateException | Missing environment variable               |

When an error occurs during the mediation process, the mediator throws a SynapseException with the corresponding error code, which can be handled by the error handling mechanism in WSO2 Micro Integrator.

## Contributing

Contributions to the XfccMediator project are welcome! If you find any issues or have suggestions for improvements, please open an issue or submit a pull request on the project's repository.

## License

The XfccMediator is licensed under Apache License 2.0. Feel free to use, modify, and distribute it according to the terms of this license.
