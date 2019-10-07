package net.feheren_fekete.applist.applistpage.viewmodel

import androidx.lifecycle.ViewModel
import net.feheren_fekete.applist.applistpage.repository.ApplistPageRepository
import org.koin.java.KoinJavaComponent.inject

class ApplistViewModel: ViewModel() {

    private val repository: ApplistPageRepository by inject(ApplistPageRepository::class.java)

    fun getItems() = repository.getItems()

}