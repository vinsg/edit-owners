package model

import kotlinx.serialization.Serializable

@Serializable
data class CodeSearch(
    val items: List<Item>
)

@Serializable
data class Item(
    val repository: Repository
)

@Serializable
data class Repository(
    val full_name: String
)

