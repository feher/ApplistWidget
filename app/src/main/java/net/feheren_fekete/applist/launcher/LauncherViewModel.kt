package net.feheren_fekete.applist.launcher

import androidx.lifecycle.ViewModel
import net.feheren_fekete.applist.launcher.model.LauncherModel
import org.koin.java.KoinJavaComponent.inject

class LauncherViewModel: ViewModel() {

    val launcherModel: LauncherModel by inject(LauncherModel::class.java)

    val launcherPages = launcherModel.pages

}
