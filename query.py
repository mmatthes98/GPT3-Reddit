import os
import openai
import json

#Returns the completion of the prompt
def getCompletion(prompt):
    openai.api_key = os.getenv("OPENAI_API_KEY")
    response = openai.Completion.create(model="davinci:ft-mmatt98-2022-06-21-20-02-46", prompt="Write the top comment for the reddit post on /r/askreddit with the following title: " + prompt, temperature=0.8, max_tokens = 400)
    print(response)
    text = response.get("choices")[0].get("text")
    text = text.replace("ENDTL", "")
    text =text.replace("END", "")
    return (text.replace("END.\n", "\n"))
