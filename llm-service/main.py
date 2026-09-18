import os
import json
import requests

from dotenv import load_dotenv
from confluent_kafka import Consumer, Producer
from reviewer import generate_review

load_dotenv ()

# Kafka Configuration matching your docker-compose setup
KAFKA_BROKER = os.getenv ('KAFKA_BROKER', 'localhost:9092')
CONSUMER_GROUP = 'llm-worker-group'

consumer = Consumer ({
    'bootstrap.servers': KAFKA_BROKER,
    'group.id': CONSUMER_GROUP,
    'auto.offset.reset': 'earliest'
})

producer = Producer ({'bootstrap.servers': KAFKA_BROKER})

def fetch_pr_diff (diff_url: str) -> str:
    """Fetches the raw Git diff from GitHub."""
    response = requests.get (diff_url, headers={'Accept': 'application/vnd.github.v3.diff'})
    response.raise_for_status ()
    return response.text

def process_events ():
    consumer.subscribe (['pr-events'])
    print ("Listening for PR events on Kafka...")

    while True:
        msg = consumer.poll (1.0)
        if msg is None:
            continue
        if msg.error ():
            print (f"Consumer error: {msg.error ()}")
            continue

        try:
            payload = json.loads (msg.value ().decode ('utf-8'))
            pr_data = payload.get ('pull_request', {})
            
            pr_title = pr_data.get ('title', 'Unknown PR')
            diff_url = pr_data.get ('diff_url')
            
            full_repo_name = payload.get ('repository', {}).get ('full_name')
            print (f"Analyzing PR: {pr_title} for {full_repo_name}")

            if not diff_url:
                continue

            git_diff = fetch_pr_diff (diff_url)
            review_json = generate_review (pr_title, git_diff)

            result_payload = {
                "pull_request_number": pr_data.get ('number'),
                "repository": full_repo_name,
                "review": json.loads (review_json)
            }

            producer.produce ('review-results', value=json.dumps (result_payload).encode ('utf-8'))
            producer.flush ()
            print ("Review completed and published to review-results topic.")

        except Exception as e:
            print (f"Error processing PR event: {e}")

if __name__ == "__main__":
    process_events ()