# Required Environment
Java 21

# Build Docker Image
Provide docker file for pipeline
Run locally:
```commandline
docker build -t tansaction-system .
```

# Architecture
DDD architecture
![architecture.png](architecture.png)

# External Libraries
Lombok: generate template code   
Junit5: for unit test   
Guava: for local cache and other util classes   

# Test Coverage
```commandline
mvn test
```
The Application layer is 100%
![coverage.png](coverage.png)

# Performance Test
Use Locust to do performance test
```commandline
cd performnace-test
pip install locust
locust -f locustfile.py --host=http://localhost:8080
```
