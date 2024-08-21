package com.salpiras.citizendocs.model

import java.time.Month
import java.time.Year
import java.util.Date

data class Document(val title : String,
                    val month: Month,
                    val year: Year,
                    val date: Date,
                    val path: String)
