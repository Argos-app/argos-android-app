package com.android.argosapp.util.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FragmentNavHelper(
    private val fragmentManager: FragmentManager,
    private val containerId: Int? = null,
) {
    fun navigate(
        fragment: Fragment,
        tag: String = fragment::class.java.simpleName,
        addToBackStack: Boolean = false,
        animations: Animations? = null,
        commitNow: Boolean = false,
    ) {
        val targetContainerId = requireNotNull(containerId) {
            "O container ID é necessário para navegar."
        }

        val transaction = fragmentManager.beginTransaction()
            .setReorderingAllowed(true)

        animations?.let {
            transaction.setCustomAnimations(it.enter, it.exit, it.popEnter, it.popExit)
        }

        transaction.replace(targetContainerId, fragment, tag)
        if (addToBackStack) transaction.addToBackStack(tag)

        if (commitNow) {
            transaction.commitNow()
        } else {
            transaction.commit()
        }
    }

    fun popBackStack() {
        fragmentManager.popBackStack()
    }

    fun clearBackStack() {
        fragmentManager.popBackStackImmediate(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
    }

    fun hasBackStack(): Boolean = fragmentManager.backStackEntryCount > 0

    fun currentFragment(): Fragment? =
        containerId?.let(fragmentManager::findFragmentById)

    fun findFragmentByTag(tag: String): Fragment? =
        fragmentManager.findFragmentByTag(tag)

    fun showBottomSheet(sheet: BottomSheetDialogFragment, tag: String) {
        sheet.show(fragmentManager, tag)
    }

    data class Animations(
        val enter: Int,
        val exit: Int,
        val popEnter: Int,
        val popExit: Int,
    )
}
