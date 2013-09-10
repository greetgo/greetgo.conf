package kz.greetgo.simpleInterpretator;

public enum ErrorCode {
  UNKNOWN, FUNCTION_WITH_NO_ARGS, UNKNOWN_CHAR, TOO_MANY_CLOSE_SKOB, TOO_MANY_OPEN_SKOB,
  UNKNOWN_FUNCTION, CANNOT_EXECUTE_FUNCTION, CANNOT_NEGATE, NOT_A_NUMBER, DIVISION_BY_ZERO,
  CANNOT_BOOLEAN;
}
