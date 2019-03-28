package org.nekobasu.core


/**
 * A Channel is a backbone communication between two ViewModels.
 * This interface is mainly a marker to indicate this special role.
 * A channel can only communicate unidirectional from child to parent,
 * or semi-bidirectional between two child ViewModels.
 */
interface Channel