/**
 * @file Host.kt
 * @description Modèle réseau représentant un hôte découvert lors d'un scan.
 */
package com.scaminal.network.model

/**
 * Représente un hôte réseau découvert.
 *
 * @param ipAddress Adresse IPv4 de l'hôte
 * @param hostname Nom d'hôte résolu (null si non résolu)
 * @param isReachable true si l'hôte a répondu au ping
 * @param openPorts Liste des ports ouverts détectés
 * @param responseTime Temps de réponse en millisecondes (-1 si non mesuré)
 */
data class Host(
    val ipAddress: String,
    val hostname: String? = null,
    val isReachable: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val responseTime: Long = -1L
)
