from locust import HttpUser, task, between
from faker import Faker

fake = Faker()

class TransactionSystemUser(HttpUser):
    wait_time = between(1, 3)

    @task(1)
    def create_transaction(self):
        payload = {
            "amount": fake.pyfloat(left_digits=3, right_digits=2, positive=True),
            "description": fake.sentence(),
            "sourceId": fake.uuid4()
        }
        response = self.client.post("/transactions", json=payload)
        if response.status_code != 201:
            print(f"Create transaction failed: {response.text}")

    @task(1)
    def delete_transaction(self):
        transaction_id = fake.uuid4()
        response = self.client.delete(f"/transactions/{transaction_id}")
        if response.status_code != 200:
            print(f"Delete transaction failed: {response.text}")

    @task(1)
    def modify_transaction(self):
        transaction_id = fake.uuid4()
        payload = {
            "amount": fake.pyfloat(left_digits=3, right_digits=2, positive=True),
            "description": fake.sentence()
        }
        response = self.client.put(f"/transactions/{transaction_id}", json=payload)
        if response.status_code != 200:
            print(f"Modify transaction failed: {response.text}")

    @task(1)
    def query_transaction_by_id(self):
        transaction_id = fake.uuid4()
        response = self.client.get(f"/transactions/{transaction_id}", json=payload)
        if response.status_code != 200:
            print(f"Query transactions by id failed: {response.text}")

    @task(2)
    def list_transactions(self):
        params = {"page": 0, "size": 10}
        response = self.client.get("/transactions", params=params)
        if response.status_code != 200:
            print(f"List transactions failed: {response.text}")
