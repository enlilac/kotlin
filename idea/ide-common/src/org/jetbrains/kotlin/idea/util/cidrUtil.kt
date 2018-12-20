/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CidrUtil")

package org.jetbrains.kotlin.idea.util

import com.intellij.util.PlatformUtils

val isRunningInCidrIde by lazy { PlatformUtils.isCidr() }