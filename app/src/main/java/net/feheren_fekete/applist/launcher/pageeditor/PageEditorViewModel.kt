package net.feheren_fekete.applist.launcher.pageeditor

import androidx.lifecycle.ViewModel
import net.feheren_fekete.applist.launcher.repository.LauncherRepository
import org.koin.java.KoinJavaComponent.inject

class PageEditorViewModel: ViewModel() {

    val launcherRepository: LauncherRepository by inject(LauncherRepository::class.java)

    val launcherPages = launcherRepository.pages

}
