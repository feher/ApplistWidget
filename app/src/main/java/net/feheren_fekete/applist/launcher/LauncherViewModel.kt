package net.feheren_fekete.applist.launcher

import androidx.lifecycle.ViewModel
import net.feheren_fekete.applist.launcher.repository.LauncherRepository
import org.koin.java.KoinJavaComponent.get

class LauncherViewModel: ViewModel() {

    val launcherRepository = get(LauncherRepository::class.java)

    val launcherPages = launcherRepository.pages

}
