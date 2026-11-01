package com.jiratools.platform

import kotlinx.coroutines.CoroutineDispatcher

/** Dispatcher dùng cho mọi thao tác vào/ra (mạng, đĩa) ngoài luồng UI. */
expect val ioDispatcher: CoroutineDispatcher
