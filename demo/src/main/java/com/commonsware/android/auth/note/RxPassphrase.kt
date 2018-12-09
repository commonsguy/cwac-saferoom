/***
 * Copyright (c) 2018 CommonsWare, LLC
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain	a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * Covered in detail in the book _The Busy Coder's Guide to Android Development_
 * https://commonsware.com/Android
 */

package com.commonsware.android.auth.note

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import okio.BufferedSink
import okio.BufferedSource
import okio.Okio

private const val BASE36_SYMBOLS = "abcdefghijklmnopqrstuvwxyz0123456789"
private const val KEYSTORE = "AndroidKeyStore"

object RxPassphrase {
  private val BLOCK_SIZE: Int

  init {
    var blockSize = -1

    try {
      blockSize = Cipher.getInstance("AES/CBC/PKCS7Padding").blockSize
    } catch (e: Exception) {
      Log.e("RxKeyBodega", "Could not get AES/CBC/PKCS7Padding cipher", e)
    }

    BLOCK_SIZE = blockSize
  }

  internal operator fun get(
    encryptedFile: File,
    keyName: String,
    timeout: Int
  ): Observable<CharArray> {
    return Observable.create(
      RxPassphrase.PassphraseObservable(
        encryptedFile,
        keyName,
        timeout
      )
    )
  }

  class PassphraseObservable(
    private val encryptedFile: File,
    private val keyName: String,
    private val timeout: Int
  ) : ObservableOnSubscribe<CharArray> {

    @Throws(Exception::class)
    override fun subscribe(emitter: ObservableEmitter<CharArray>) {
      val ks = KeyStore.getInstance(KEYSTORE)

      ks.load(null)

      if (encryptedFile.exists()) {
        load(ks, emitter)
      } else {
        create(ks, emitter)
      }
    }

    @Throws(Exception::class)
    private fun create(ks: KeyStore, emitter: ObservableEmitter<CharArray>) {
      val rand = SecureRandom()
      val passphrase = CharArray(128)

      for (i in passphrase.indices) {
        passphrase[i] = BASE36_SYMBOLS[rand.nextInt(BASE36_SYMBOLS.length)]
      }

      createKey(ks, keyName, timeout)

      val secretKey = ks.getKey(keyName, null) as SecretKey
      val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
      val iv = ByteArray(BLOCK_SIZE)

      rand.nextBytes(iv)

      val ivParams = IvParameterSpec(iv)

      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams)

      val toEncrypt = toBytes(passphrase)
      val encrypted = cipher.doFinal(toEncrypt)

      val sink = Okio.buffer(Okio.sink(encryptedFile))

      sink.write(iv)
      sink.write(encrypted)
      sink.close()

      emitter.onNext(passphrase)
    }

    @Throws(Exception::class)
    private fun createKey(ks: KeyStore, keyName: String, timeout: Int) {
      val entry = ks.getEntry(keyName, null)

      if (entry == null) {
        val spec = KeyGenParameterSpec.Builder(
          keyName,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
          .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
          .setUserAuthenticationRequired(true)
          .setUserAuthenticationValidityDurationSeconds(timeout)
          .setRandomizedEncryptionRequired(false)
          .build()

        val keygen =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)

        keygen.init(spec)
        keygen.generateKey()
      }
    }

    @Throws(Exception::class)
    private fun load(ks: KeyStore, emitter: ObservableEmitter<CharArray>) {
      val source = Okio.buffer(Okio.source(encryptedFile))
      val iv = source.readByteArray(BLOCK_SIZE.toLong())
      val encrypted = source.readByteArray()

      source.close()

      val secretKey = ks.getKey(keyName, null) as SecretKey
      val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

      cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

      val decrypted = cipher.doFinal(encrypted)
      val passphrase = toChars(decrypted)

      emitter.onNext(passphrase)
    }
  }

  // based on https://stackoverflow.com/a/9670279/115145

  internal fun toBytes(chars: CharArray): ByteArray {
    val charBuffer = CharBuffer.wrap(chars)
    val byteBuffer = Charset.forName("UTF-8").encode(charBuffer)
    val bytes = Arrays.copyOfRange(
      byteBuffer.array(), byteBuffer.position(),
      byteBuffer.limit()
    )

    //    Arrays.fill(charBuffer.array(), '\u0000'); // clear the cleartext
    Arrays.fill(byteBuffer.array(), 0.toByte()) // clear the ciphertext

    return bytes
  }

  internal fun toChars(bytes: ByteArray): CharArray {
    val charset = Charset.forName("UTF-8")
    val byteBuffer = ByteBuffer.wrap(bytes)
    val charBuffer = charset.decode(byteBuffer)
    val chars = Arrays.copyOf(charBuffer.array(), charBuffer.limit())

    Arrays.fill(charBuffer.array(), '\u0000') // clear the cleartext
    Arrays.fill(byteBuffer.array(), 0.toByte()) // clear the ciphertext

    return chars
  }
}

