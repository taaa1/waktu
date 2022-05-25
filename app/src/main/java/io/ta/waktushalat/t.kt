package io.ta.waktushalat

class t(
    val features: List<feature>
)

class feature(val properties: props?, val geometry: geometry)

class props(val name: String?, val country: String?)

class geometry(val coordinates: List<Float>)