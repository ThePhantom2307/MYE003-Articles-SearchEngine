import os
import torch
import pandas as pd
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer
from langchain.text_splitter import RecursiveCharacterTextSplitter

INDEX_FILE = "articles_index.faiss"
CHUNKS_FILE = "chunk_metadata.csv"
CSV_FILE = "CNN_Articles_clean.csv"

def generate_faiss_and_chunks():
    if os.path.exists(INDEX_FILE) and os.path.exists(CHUNKS_FILE): #Tsekaroume an yparxoun hdh
        return

    df = pd.read_csv(CSV_FILE) #Diavazoume to CSV
    articles = df["Article text"].dropna().tolist() #Kratame to pedio Article text gia na paroume ta arthra

    text_splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=100)
    
    chunks = [] #Pinakas gia ta chunks
    for article in articles:
        chunks.extend(text_splitter.split_text(article)) #Prosthetoume ta chunks pou mas dinei o text_splitter

    device = "cuda" if torch.cuda.is_available() else "cpu" #Dinoume thn epilogh gia dhmkourgia twn batches me cuda an yparxei
    print("Using device:", device) #DEBUGGING

    model = SentenceTransformer('all-MiniLM-L6-v2', device=device) #Epilegoume to model kai to device
    embeddings = model.encode(chunks, show_progress_bar=True) #Dhmiourgoume ta embeddings gia kathe chunk
    dimension = embeddings.shape[1] #Kratame to dimension twn embeddings

    index = faiss.IndexFlatL2(dimension) #Dhmiourgoume to faiss index
    index.add(np.array(embeddings)) #Prosthetoume ta embeddings sto index
    faiss.write_index(index, INDEX_FILE) #Grafoyme to index se arxeio

    chunk_metadata = pd.DataFrame({"chunk": chunks})
    chunk_metadata.to_csv(CHUNKS_FILE, index=False)
