from tkinter import Tk, Label, Entry, Button, Text, END, Scrollbar, VERTICAL, RIGHT, Y, DISABLED, NORMAL
from rag_data_preparation import generate_faiss_and_chunks
from query_engine import answer_query
import os
import threading

INDEX_FILE = "articles_index.faiss"
CHUNKS_FILE = "chunk_metadata.csv"

def run_query(query):
    try:
        response, used_chunks = answer_query(query)

        def update_ui():
            text_output.delete("1.0", END)
            text_output.insert(END, "➤ Response:\n")
            text_output.insert(END, response)

            text_output.insert(END, "\n\n➤ Relevant chunks:\n")
            for i, chunk in enumerate(used_chunks, start=1):
                text_output.insert(END, f"\n[{i}] {chunk}\n")

            submit_button.config(state=NORMAL)
            entry.config(state=NORMAL)
            entry.delete(0, END)
            entry.focus()

        root.after(0, update_ui)

    except Exception as e:
        def show_error():
            text_output.insert(END, f"\nError:\n{str(e)}")
            submit_button.config(state=NORMAL)
        root.after(0, show_error)


def process_query(event=None):
    query = entry.get().strip()
    if not query:
        return

    text_output.delete("1.0", END)
    text_output.insert(END, "Searching...\n")
    submit_button.config(state=DISABLED)
    entry.config(state=DISABLED)

    threading.Thread(target=run_query, args=(query,), daemon=True).start()

def run_init():
        try:
            generate_faiss_and_chunks()
            def finish():
                text_output.insert(END, "Initialization completed. Enter your prompt.\n")
                submit_button.config(state=NORMAL)
            root.after(0, finish)
        except Exception as e:
            root.after(0, lambda: text_output.insert(END, f"\nInitialization failed:\n{str(e)}"))

def initialize_data():
    text_output.insert(END, "Initializing FAISS index and chunks...\n")
    submit_button.config(state=DISABLED)
    threading.Thread(target=run_init, daemon=True).start()

def launch_gui():
    global root, entry, text_output, submit_button

    root = Tk()
    root.title("RAG AI Assistant")
    root.geometry("700x500")

    Label(root, text="Enter your prompt:", font=("Arial", 12)).pack(pady=5)

    entry = Entry(root, width=80)
    entry.pack(pady=5)
    entry.bind("<Return>", process_query)

    submit_button = Button(root, text="Submit", command=process_query)
    submit_button.pack(pady=5)

    scrollbar = Scrollbar(root, orient=VERTICAL)
    scrollbar.pack(side=RIGHT, fill=Y)

    text_output = Text(root, wrap="word", yscrollcommand=scrollbar.set)
    text_output.pack(expand=True, fill="both")
    scrollbar.config(command=text_output.yview)

    if not os.path.exists(INDEX_FILE) or not os.path.exists(CHUNKS_FILE):
        initialize_data()
    else:
        text_output.insert(END, "Data already initialized. Enter your prompt.\n")

    root.mainloop()

if __name__ == "__main__":
    launch_gui()
