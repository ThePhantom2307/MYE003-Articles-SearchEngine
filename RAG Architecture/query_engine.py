import faiss
import pandas as pd
import numpy as np
from sentence_transformers import SentenceTransformer
from ollama import Client

INDEX_FILE = "articles_index.faiss"
CHUNKS_FILE = "chunk_metadata.csv"
embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
client = Client(host='http://localhost:11434')

def build_prompt(query, retrieved_chunks): #Ftiaxnoume to prompt
    context = "\n\n".join(retrieved_chunks)
    return (
        f"You are an AI assistant. Answer the following question:\n"
        f"{query}\n\n"
        f"based on the following context:\n\n"
        f"{context}"
    )

def answer_query(user_query, model_name="llama3.2:3b", k=5):
    index = faiss.read_index(INDEX_FILE)
    chunks_df = pd.read_csv(CHUNKS_FILE)

    query_embedding = embedding_model.encode([user_query])
    distances, indices = index.search(np.array(query_embedding), k)
    top_chunks = [chunks_df.iloc[idx]["chunk"] for idx in indices[0]]
    prompt = build_prompt(user_query, top_chunks)

    response = client.generate(model=model_name, prompt=prompt)
    return response['response'].strip(), top_chunks
