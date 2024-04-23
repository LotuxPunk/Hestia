# Hestia

Hestia is a simple file manipulation service, it allows you to upload, download, delete and list files via a REST API.

This project intends to be a reliable and simple solution for small scale file manipulation and avoid the need to setup a full fledged cloud storage service.

# Notice

- Inter-server communication, since it's an api key based authentication, the key should be kept secret and not exposed to the client side.
- It is intended to be used with small/constrained file sizes, the file you'll try to manipulate will be loaded into memory and then sent to the server. For large files, it's recommended to use a more robust solution like [AWS S3](https://aws.amazon.com/s3/), [Google Cloud Storage](https://cloud.google.com/storage) or [Azure Blob Storage](https://azure.microsoft.com/en-us/services/storage/blobs/).
