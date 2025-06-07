package com.example.cash

data class GptResponse(val choices: List<Choice>)
data class Choice(val message: Message)
