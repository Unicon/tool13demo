function Header (props) {

  return (
    <>
      <h1>{props.header}</h1>
      <p className="text-dark">{props.subheader}</p>
    </>
  );

}

export default Header;
