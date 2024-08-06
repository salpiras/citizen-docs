package com.salpiras.citizendocs.ui.list

import androidx.lifecycle.ViewModel
import com.salpiras.citizendocs.model.DocsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BookshelfViewModel @Inject constructor(private val repo: DocsRepository) : ViewModel() {

}