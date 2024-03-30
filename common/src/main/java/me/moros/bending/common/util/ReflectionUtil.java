/*
 * Copyright 2020-2024 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unchecked")
public final class ReflectionUtil {
  private ReflectionUtil() {
  }

  public static <T> @Nullable T findAndCreateInnerClass(Object parent, Class<T> clazz) {
    for (var innerClass : parent.getClass().getDeclaredClasses()) {
      if (clazz.isAssignableFrom(innerClass)) {
        try {
          Constructor<?> constructor = innerClass.getDeclaredConstructor();
          constructor.setAccessible(true);
          return (T) constructor.newInstance();
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
      }
    }
    return null;
  }

  public static Class<?> getClassOrThrow(String fullName) {
    try {
      return Class.forName(fullName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static <V> void injectStatic(Class<?> clazz, V value) {
    var valueClass = value.getClass();
    for (var field : clazz.getDeclaredFields()) {
      if (!isFinal(field) && field.getType().isAssignableFrom(valueClass)) {
        accessField(field, f -> {
          f.set(null, value);
          return f;
        });
        return;
      }
    }
  }

  public static <V> V getStaticFieldOrThrow(Class<?> clazz, String fieldName) {
    return getFieldAs(clazz, fieldName, null);
  }

  private static <T, V> V getFieldAs(Class<?> clazz, String fieldName, @Nullable T instance) {
    try {
      var field = getFieldSafe(clazz, fieldName);
      return (V) accessField(field, f -> f.get(instance)).orElseThrow();
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static Field getFieldSafe(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    return clazz.getDeclaredField(fieldName);
  }

  private static <R> Optional<R> accessField(Field field, FieldFunction<R> function) {
    field.setAccessible(true);
    try {
      return Optional.of(function.apply(field));
    } catch (IllegalAccessException e) {
      return Optional.empty();
    }
  }

  private static boolean isFinal(Field field) {
    return (field.getModifiers() & Modifier.FINAL) == Modifier.FINAL;
  }

  @FunctionalInterface
  private interface FieldFunction<R> {
    R apply(Field field) throws IllegalAccessException;
  }
}
