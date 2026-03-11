/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Binder;
import android.os.UserHandle;

/**
 * Represents a verified calling package with its associated UID and package name.
 *
 * <p>Use {@link #get(Context, String)} to create an instance from a caller-provided package name,
 * which validates that the package name belongs to the actual calling UID.
 */
public final class CallingPackage {
    private final int mUid;
    @Nullable
    private final String mPackageName;

    public CallingPackage(int uid, @Nullable String packageName) {
        this.mUid = uid;
        this.mPackageName = packageName;
    }

    /** @return the UID associated with this calling package. */
    public int uid() {
        return mUid;
    }

    /** @return the verified package name, or null if unknown. */
    @Nullable
    public String packageName() {
        return mPackageName;
    }

    /** @return the UserHandle derived from the UID. */
    @NonNull
    public UserHandle userHandle() {
        return UserHandle.getUserHandleForUid(mUid);
    }

    /**
     * Creates a {@link CallingPackage} by validating the given package name against the
     * calling UID using AppOpsManager.
     *
     * @param context the context for AppOpsManager access
     * @param unverifiedPackageName the package name provided by the caller (may be null or spoofed)
     * @return a verified {@link CallingPackage} instance
     */
    @NonNull
    public static CallingPackage get(@NonNull Context context,
            @Nullable String unverifiedPackageName) {
        int callingUid = Binder.getCallingUid();
        if (unverifiedPackageName != null) {
            try {
                AppOpsManager appOps =
                        (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOps != null) {
                    appOps.checkPackage(callingUid, unverifiedPackageName);
                    return new CallingPackage(callingUid, unverifiedPackageName);
                }
            } catch (SecurityException e) {
                // Package name doesn't match calling UID; fall through to UID-based lookup
            }
        }
        // Fall back to deriving package name from UID
        String[] packages = context.getPackageManager().getPackagesForUid(callingUid);
        String pkgName = (packages != null && packages.length > 0) ? packages[0] : null;
        return new CallingPackage(callingUid, pkgName);
    }

    @Override
    public String toString() {
        return "CallingPackage{uid=" + mUid + ", packageName=" + mPackageName + "}";
    }
}
