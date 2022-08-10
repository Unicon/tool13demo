function Header (props) {

  return (
    <>
      <p className="header-title">{props.header}</p>
      <p className="header-subheader">{props.subheader}</p>
    </>
  );

}

export default Header;
