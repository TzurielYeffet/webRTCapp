package com.example.webrtcapp.models

data class IceCandidateModel(
    val sdpMid:String,
    val sdpMLineIndex:Double,
    val sdpCandidate:String
)