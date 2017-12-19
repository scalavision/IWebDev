package iwebdev.server

object StylesheetSample {

  val css =
    """
      |.intro {
      |  outline: auto;
      |  outline: dashed;
      |}
    """.stripMargin

  val smallStyleSheet =
    """
      |.intro {
      |  outline: auto;
      |  outline: dashed;
      |}
      |
      |
      |ul:link {
      |  outline: double;
      |  outline: dashed;
      |  display: flex;
      |}
    """.stripMargin

  val styleSheet =
    """
      |
      |.intro {
      |  outline: auto;
      |  outline: dashed;
      |}
      |
      |
      |ul:link {
      |  outline: double;
      |  outline: dashed;
      |}
      |
      |
      |ul:link::after::before {
      |  outline: double;
      |  outline: dashed;
      |}
      |
      |
      |.intro > ul > li {
      |  outline: auto;
      |  outline: dashed;
      |  outline: double;
      |}
      |
      |
      |.intro:link > ul::after > li::before {
      |  outline: auto;
      |  outline: dashed;
      |  outline: double;
      |}
      |
      |
      |ul.intro {
      |  outline: auto;
      |  outline: dashed;
      |  outline: double;
      |}
      |
      |
      |ul::before::after:hover:focus > .intro:link > ul > .intro {
      |  outline: double;
      |  outline: dashed;
      |}
      |
      |
      |@media (max-color : 10) {
      |
      |.intro > ul {
      |  outline: double;
      |  outline: dashed;
      |  outline: double;
      |}
      |
      |
      |ul {
      |  outline: dashed;
      |  outline: inset;
      |}
      |
      |
      |ul.intro > li {
      |  outline: auto;
      |  outline: double;
      |  outline: medium;
      |}
      |
      |
      |ul.intro {
      |  outline: auto;
      |  outline: double;
      |  outline: medium;
      |}
      |
      |}
      |
      |
      |.ripple {
      |  position: absolute;
      |  background-color: rgba(0, 0, 0, 0.25);
      |  border-radius: 100%;
      |  transform: scale(0.2);
      |  opacity: 0;
      |  pointer-events: none;
      |  animation: ripple-animation;0.75s;ease-out;
      |}
      |
      |
      |ul {
      |  outline: double;
      |  font-size: 10px;
      |}
      |
      |
      |li {
      |  outline: double;
      |  font-size: 10px;
      |}
      |
      |
      |h1 {
      |  outline: double;
      |  font-size: 10px;
      |}
      |
      |
      |h2 {
      |  outline: double;
      |  font-size: 10px;
      |}
      |
      |
      |h3 {
      |  outline: double;
      |  font-size: 10px;
      |}
      |
      |
      |ul {
      |  outline: dashed;
      |}
      |
      |
      |.intro {
      |  outline: dashed;
      |}
      |
      |
      |li > .intro {
      |  outline: dashed;
      |}
      |
      |
      |a.intro + .intro {
      |  outline: dashed;
      |}
      |
      |
      |b ~ .intro {
      |  outline: dashed;
      |}
      |
    """.stripMargin

}
