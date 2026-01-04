if ! ollama list | grep -q "qwen2.1:5b"; then
    ollama pull qwen2.1:5b
fi

exec ollama serve
