/**
 * @file ScanState.kt
 * @description États possibles d'un scan réseau (IP ou ports).
 */
package com.scaminal.network.model

/**
 * Représente l'état courant d'une opération de scan.
 */
sealed class ScanState {
    /** Aucun scan en cours */
    data object Idle : ScanState()

    /**
     * Scan en cours.
     *
     * @param progress Progression de 0 à 100
     */
    data class InProgress(val progress: Int = 0) : ScanState()

    /**
     * Scan terminé avec succès.
     *
     * @param hostsFound Nombre d'hôtes découverts
     */
    data class Completed(val hostsFound: Int = 0) : ScanState()

    /**
     * Scan échoué.
     *
     * @param message Description de l'erreur
     */
    data class Error(val message: String) : ScanState()
}
