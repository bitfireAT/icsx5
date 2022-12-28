package at.bitfire.icsdroid.exceptions

import java.io.IOException

abstract class HttpException(msg: String): IOException(msg)
