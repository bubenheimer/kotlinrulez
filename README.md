# Kotlin Rulez
#### An efficient rule engine for Kotlin with Coroutines

Ready for production use.

I have found the rule engine a great pattern for Android app initialization handling,
which commonly has to deal with somewhat complex asynchronous process flow and event handling
for API initialization. It is a generic rule engine, so it can be used for many other purposes.

This rule engine is inspired by the ["Java Rulez" engine](https://github.com/bubenheimer/javarulez)
by the same author, and uses the same style of rule matching via bitmask operations. Otherwise the
Kotlin-based approach is a complete rewrite and is *much* nicer than its inspiration.

Regarding the original Java Rulez engine, please see the
[blog entry](http://android.bubenheimer.com/2016/02/android-rulez-efficient-rule-engine-for.html)
for more information including a presentation from DevFest MN 2016.

The library uses inline classes and unsigned types at its core, so these experimental Kotlin
features need to be enabled for library consumers as well. The library can be built against
Kotlin 1.4.0 and Coroutines 1.3.9, as well as Kotlin 1.3.72 and Coroutines 1.3.8, but it does not
currently seem possible to consume either library configuration in a project with the other
configuration.

License
-------

    Copyright (c) 2015-2020 Uli Bubenheimer

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
